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

package bisq.core.trade.protocol;


import bisq.core.trade.messages.CreateAtomicTxRequest;
import bisq.core.trade.protocol.tasks.maker.AtomicMakerCreatesAndSignsTx;
import bisq.core.trade.protocol.tasks.maker.AtomicMakerSetupTxListener;
import bisq.core.trade.protocol.tasks.maker.AtomicMakerVerifiesTakerInputs;
import bisq.core.util.Validator;

import bisq.network.p2p.NodeAddress;

import bisq.common.handlers.ErrorMessageHandler;

public interface AtomicMakerProtocol {
    default void handleTakeAtomicRequest(CreateAtomicTxRequest tradeMessage,
                                         NodeAddress sender,
                                         ErrorMessageHandler errorMessageHandler) {
        Validator.checkTradeId(((TradeProtocol) this).processModel.getOfferId(), tradeMessage);
        ((TradeProtocol) this).processModel.setTradeMessage(tradeMessage);
        ((TradeProtocol) this).processModel.setTempTradingPeerNodeAddress(sender);

        TradeTaskRunner taskRunner = new TradeTaskRunner(((TradeProtocol) this).trade,
                () -> ((TradeProtocol) this).handleTaskRunnerSuccess(tradeMessage, "handleTakeAtomicRequest"),
                errorMessage -> {
                    errorMessageHandler.handleErrorMessage(errorMessage);
                    ((TradeProtocol) this).handleTaskRunnerFault(tradeMessage, errorMessage);
                });

        taskRunner.addTasks(
                AtomicMakerVerifiesTakerInputs.class,
                AtomicMakerCreatesAndSignsTx.class,
                AtomicMakerSetupTxListener.class
        );

        taskRunner.run();
    }
}