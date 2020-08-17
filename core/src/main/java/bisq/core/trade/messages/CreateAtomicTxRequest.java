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

package bisq.core.trade.messages;

import bisq.core.btc.model.RawTransactionInput;

import bisq.network.p2p.DirectMessage;
import bisq.network.p2p.NodeAddress;

import bisq.common.app.Version;
import bisq.common.crypto.PubKeyRing;

import java.util.List;
import java.util.stream.Collectors;

import lombok.EqualsAndHashCode;
import lombok.Value;

@EqualsAndHashCode(callSuper = true)
@Value
public final class CreateAtomicTxRequest extends TradeMessage implements DirectMessage {
    private final NodeAddress senderNodeAddress;
    private final PubKeyRing takerPubKeyRing;
    private final long bsqTradeAmount;
    private final long btcTradeAmount;
    private final long tradePrice;
    private final long txFee;
    private final long takerFee;
    private final boolean isCurrencyForTakerFeeBtc;
    private final long takerBsqOutputValue;
    private final String takerBsqOutputAddress;
    private final long takerBtcOutputValue;
    private final String takerBtcOutputAddress;
    private final List<RawTransactionInput> takerBsqInputs;
    private final List<RawTransactionInput> takerBtcInputs;

    public CreateAtomicTxRequest(String uid,
                                 String tradeId,
                                 NodeAddress senderNodeAddress,
                                 PubKeyRing takerPubKeyRing,
                                 long bsqTradeAmount,
                                 long btcTradeAmount,
                                 long tradePrice,
                                 long txFee,
                                 long takerFee,
                                 boolean isCurrencyForTakerFeeBtc,
                                 long takerBsqOutputValue,
                                 String takerBsqOutputAddress,
                                 long takerBtcOutputValue,
                                 String takerBtcOutputAddress,
                                 List<RawTransactionInput> takerBsqInputs,
                                 List<RawTransactionInput> takerBtcInputs) {
        this(Version.getP2PMessageVersion(),
                uid,
                tradeId,
                senderNodeAddress,
                takerPubKeyRing,
                bsqTradeAmount,
                btcTradeAmount,
                tradePrice,
                txFee,
                takerFee,
                isCurrencyForTakerFeeBtc,
                takerBsqOutputValue,
                takerBsqOutputAddress,
                takerBtcOutputValue,
                takerBtcOutputAddress,
                takerBsqInputs,
                takerBtcInputs);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////
    // PROTO BUFFER
    ///////////////////////////////////////////////////////////////////////////////////////////

    private CreateAtomicTxRequest(int messageVersion,
                                  String uid,
                                  String tradeId,
                                  NodeAddress senderNodeAddress,
                                  PubKeyRing takerPubKeyRing,
                                  long bsqTradeAmount,
                                  long btcTradeAmount,
                                  long tradePrice,
                                  long txFee,
                                  long takerFee,
                                  boolean isCurrencyForTakerFeeBtc,
                                  long takerBsqOutputValue,
                                  String takerBsqOutputAddress,
                                  long takerBtcOutputValue,
                                  String takerBtcOutputAddress,
                                  List<RawTransactionInput> takerBsqInputs,
                                  List<RawTransactionInput> takerBtcInputs) {
        super(messageVersion, tradeId, uid);
        this.senderNodeAddress = senderNodeAddress;
        this.takerPubKeyRing = takerPubKeyRing;
        this.bsqTradeAmount = bsqTradeAmount;
        this.btcTradeAmount = btcTradeAmount;
        this.tradePrice = tradePrice;
        this.txFee = txFee;
        this.takerFee = takerFee;
        this.isCurrencyForTakerFeeBtc = isCurrencyForTakerFeeBtc;
        this.takerBsqOutputValue = takerBsqOutputValue;
        this.takerBsqOutputAddress = takerBsqOutputAddress;
        this.takerBtcOutputValue = takerBtcOutputValue;
        this.takerBtcOutputAddress = takerBtcOutputAddress;
        this.takerBsqInputs = takerBsqInputs;
        this.takerBtcInputs = takerBtcInputs;
    }

    @Override
    public protobuf.NetworkEnvelope toProtoNetworkEnvelope() {
        return getNetworkEnvelopeBuilder()
                .setCreateAtomicTxRequest(protobuf.CreateAtomicTxRequest.newBuilder()
                        .setUid(uid)
                        .setTradeId(tradeId)
                        .setSenderNodeAddress(senderNodeAddress.toProtoMessage())
                        .setTakerPubKeyRing(takerPubKeyRing.toProtoMessage())
                        .setBsqTradeAmount(bsqTradeAmount)
                        .setBtcTradeAmount(btcTradeAmount)
                        .setTradePrice(tradePrice)
                        .setTxFee(txFee)
                        .setTakerFee(takerFee)
                        .setIsCurrencyForTakerFeeBtc(isCurrencyForTakerFeeBtc)
                        .setTakerBsqOutputValue(takerBsqOutputValue)
                        .setTakerBsqOutputAddress(takerBsqOutputAddress)
                        .setTakerBtcOutputValue(takerBtcOutputValue)
                        .setTakerBtcOutputAddress(takerBtcOutputAddress)
                        .addAllTakerBsqInputs(takerBsqInputs.stream().map(RawTransactionInput::toProtoMessage).collect(
                                Collectors.toList()))
                        .addAllTakerBtcInputs(takerBtcInputs.stream().map(RawTransactionInput::toProtoMessage).collect(
                                Collectors.toList()))
                ).build();
    }

    public static CreateAtomicTxRequest fromProto(protobuf.CreateAtomicTxRequest proto, int messageVersion) {
        return new CreateAtomicTxRequest(messageVersion,
                proto.getUid(),
                proto.getTradeId(),
                NodeAddress.fromProto(proto.getSenderNodeAddress()),
                PubKeyRing.fromProto(proto.getTakerPubKeyRing()),
                proto.getBsqTradeAmount(),
                proto.getBtcTradeAmount(),
                proto.getTradePrice(),
                proto.getTxFee(),
                proto.getTakerFee(),
                proto.getIsCurrencyForTakerFeeBtc(),
                proto.getTakerBsqOutputValue(),
                proto.getTakerBsqOutputAddress(),
                proto.getTakerBtcOutputValue(),
                proto.getTakerBtcOutputAddress(),
                proto.getTakerBsqInputsList().stream()
                        .map(RawTransactionInput::fromProto)
                        .collect(Collectors.toList()),
                proto.getTakerBtcInputsList().stream()
                        .map(RawTransactionInput::fromProto)
                        .collect(Collectors.toList())
        );
    }

    @Override
    public String toString() {
        return "CreateAtomicTxRequest{" +
                "\n     senderNodeAddress=" + senderNodeAddress +
                "\n     takerPubKeyRing=" + takerPubKeyRing +
                "\n     bsqTradeAmount=" + bsqTradeAmount +
                "\n     btcTradeAmount=" + btcTradeAmount +
                "\n     tradePrice=" + tradePrice +
                "\n     txFee=" + txFee +
                "\n     takerFee=" + takerFee +
                "\n     isCurrencyForTakerFeeBtc=" + isCurrencyForTakerFeeBtc +
                "\n     takerBsqOutputValue=" + takerBsqOutputValue +
                "\n     takerBsqOutputAddress=" + takerBsqOutputAddress +
                "\n     takerBtcOutputValue=" + takerBtcOutputValue +
                "\n     takerBtcOutputAddress=" + takerBtcOutputAddress +
                "\n     takerBsqInputs=" + takerBsqInputs +
                "\n     takerBtcInputs=" + takerBtcInputs +
                "\n} " + super.toString();
    }
}