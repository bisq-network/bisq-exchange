/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.core.trade.statistics;

import bisq.core.offer.availability.DisputeAgentSelection;

import bisq.network.p2p.BootstrapListener;
import bisq.network.p2p.P2PService;
import bisq.network.p2p.storage.P2PDataStorage;
import bisq.network.p2p.storage.payload.PersistableNetworkPayload;
import bisq.network.p2p.storage.persistence.AppendOnlyDataStoreService;

import bisq.common.config.Config;
import bisq.common.file.FileUtil;

import com.google.inject.Inject;

import javax.inject.Named;
import javax.inject.Singleton;

import java.io.File;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
public class TradeStatisticsConverter {

    @Inject
    public TradeStatisticsConverter(P2PService p2PService,
                                    P2PDataStorage p2PDataStorage,
                                    TradeStatistics2StorageService tradeStatistics2StorageService,
                                    TradeStatistics3StorageService tradeStatistics3StorageService,
                                    AppendOnlyDataStoreService appendOnlyDataStoreService,
                                    @Named(Config.STORAGE_DIR) File storageDir) {
        File tradeStatistics2Store = new File(storageDir, "TradeStatistics2Store");
        appendOnlyDataStoreService.addService(tradeStatistics2StorageService);

        p2PService.addP2PServiceListener(new BootstrapListener() {
            @Override
            public void onTorNodeReady() {
                if (!tradeStatistics2Store.exists()) {
                    return;
                }

                // We convert early once tor is initialized but still not ready to receive data
                var mapOfLiveData = tradeStatistics3StorageService.getMapOfLiveData();
                convertToTradeStatistics3(tradeStatistics2StorageService.getMapOfAllData().values())
                        .forEach(e -> mapOfLiveData.put(new P2PDataStorage.ByteArray(e.getHash()), e));
                tradeStatistics3StorageService.persistNow();
                try {
                    log.info("We delete now the old trade statistics file as it was converted to the new format.");
                    FileUtil.deleteFileIfExists(tradeStatistics2Store);
                } catch (IOException e) {
                    e.printStackTrace();
                    log.error(e.toString());
                }
            }

            @Override
            public void onUpdatedDataReceived() {
            }
        });

        // We listen to old TradeStatistics2 objects, convert and store them and rebroadcast.
        p2PDataStorage.addAppendOnlyDataStoreListener(payload -> {
            if (payload instanceof TradeStatistics2) {
                TradeStatistics3 tradeStatistics3 = convertToTradeStatistics3((TradeStatistics2) payload, true);
                // We add it to the p2PDataStorage, which handles to get the data stored in the maps and maybe
                // re-broadcast as tradeStatistics3 object if not already received.
                p2PDataStorage.addPersistableNetworkPayload(tradeStatistics3, null, true);
            }
        });
    }

    private static List<TradeStatistics3> convertToTradeStatistics3(Collection<PersistableNetworkPayload> persistableNetworkPayloads) {
        List<TradeStatistics3> list = new ArrayList<>();
        long ts = System.currentTimeMillis();

        // We might have duplicate entries from both traders as the trade date was different from old clients.
        // This should not be the case with converting old persisted data as we did filter those out but it is the case
        // when we receive old trade stat objects from the network of 2 not updated traders.
        // The hash was ignoring the trade date so we use that to get a unique list
        Map<P2PDataStorage.ByteArray, TradeStatistics2> mapWithoutDuplicates = new HashMap<>();
        persistableNetworkPayloads.stream()
                .filter(e -> e instanceof TradeStatistics2)
                .map(e -> (TradeStatistics2) e)
                .filter(TradeStatistics2::isValid)
                .forEach(e -> mapWithoutDuplicates.putIfAbsent(new P2PDataStorage.ByteArray(e.getHash()), e));

        log.info("We convert the existing {} trade statistics objects to the new format. " +
                "This might take a bit but is only done once.", mapWithoutDuplicates.size());

        mapWithoutDuplicates.values().stream()
                .map(e -> convertToTradeStatistics3(e, false))
                .filter(TradeStatistics3::isValid)
                .forEach(list::add);

        log.info("Conversion to {} new trade statistic objects has been completed after {} ms",
                list.size(), System.currentTimeMillis() - ts);

        // We prune mediator and refundAgent data from all objects but the last 100 as we only use the
        // last 100 entries (DisputeAgentSelection.LOOK_BACK_RANGE).
        list.sort(Comparator.comparing(TradeStatistics3::getDate));
        for (int i = list.size() - DisputeAgentSelection.LOOK_BACK_RANGE; i < list.size(); i++) {
            TradeStatistics3 tradeStatistics3 = list.get(i);
            tradeStatistics3.pruneOptionalData();
        }

        return list;
    }

    private static TradeStatistics3 convertToTradeStatistics3(TradeStatistics2 tradeStatistics2, boolean fromNetwork) {
        Map<String, String> extraDataMap = tradeStatistics2.getExtraDataMap();
        String mediator = extraDataMap != null ? extraDataMap.get(TradeStatistics2.MEDIATOR_ADDRESS) : null;
        String refundAgent = extraDataMap != null ? extraDataMap.get(TradeStatistics2.REFUND_AGENT_ADDRESS) : null;
        long time = tradeStatistics2.getTradeDate().getTime();
        byte[] hash = null;
        if (fromNetwork) {
            // We need to avoid that we duplicate tradeStatistics2 objects in case both traders have not udpated yet.
            // Before v1.4.0 both traders published the trade statistics. If one trader has updated he will check
            // the capabilities of the peer and if the peer has not updated he will leave publishing to the peer, so we
            // do not have the problem of duplicated objects.
            // To ensure we add only one object we will use the hash of the tradeStatistics2 object which is the same
            // for both traders as it excluded the trade date which is different for both.
            hash = tradeStatistics2.getHash();
        }
        return new TradeStatistics3(tradeStatistics2.getCurrencyCode(),
                tradeStatistics2.getTradePrice().getValue(),
                tradeStatistics2.getTradeAmount().getValue(),
                tradeStatistics2.getOfferPaymentMethod(),
                time,
                mediator,
                refundAgent,
                hash);
    }
}