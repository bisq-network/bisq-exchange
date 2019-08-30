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

package bisq.core.dispute.mediator;

import bisq.core.dispute.DisputeResolverService;
import bisq.core.filter.FilterManager;

import bisq.network.p2p.NodeAddress;
import bisq.network.p2p.P2PService;

import javax.inject.Inject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;


@Slf4j
public class MediatorService extends DisputeResolverService<Mediator> {
    @Inject
    public MediatorService(P2PService p2PService, FilterManager filterManager) {
        super(p2PService, filterManager);
    }

    @Override
    protected Set<Mediator> getCollect(List<String> bannedDisputeResolvers) {
        return p2PService.getDataMap().values().stream()
                .filter(data -> data.getProtectedStoragePayload() instanceof Mediator)
                .map(data -> (Mediator) data.getProtectedStoragePayload())
                .filter(a -> bannedDisputeResolvers == null ||
                        !bannedDisputeResolvers.contains(a.getNodeAddress().getFullAddress()))
                .collect(Collectors.toSet());
    }

    @Override
    protected List<String> getDisputeResolversFromFilter() {
        return filterManager.getFilter() != null ? filterManager.getFilter().getMediators() : new ArrayList<>();
    }

    public Map<NodeAddress, Mediator> getMediators() {
        return super.getDisputeResolvers();
    }
}
