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

package bisq.core.trade.protocol.tasks.buyer.cancel;

import bisq.core.trade.BuyerTrade;
import bisq.core.trade.Trade;
import bisq.core.trade.messages.cancel.CancelTradeRequestRejectedMessage;
import bisq.core.trade.protocol.tasks.TradeTask;
import bisq.core.util.Validator;

import bisq.common.taskrunner.TaskRunner;

import lombok.extern.slf4j.Slf4j;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

@Slf4j
public class ProcessCancelTradeRequestRejectedMessage extends TradeTask {
    @SuppressWarnings({"unused"})
    public ProcessCancelTradeRequestRejectedMessage(TaskRunner taskHandler, Trade trade) {
        super(taskHandler, trade);
    }

    @Override
    protected void run() {
        try {
            runInterceptHook();
            CancelTradeRequestRejectedMessage message = (CancelTradeRequestRejectedMessage) processModel.getTradeMessage();
            Validator.checkTradeId(processModel.getOfferId(), message);
            checkNotNull(message);

            checkArgument(!trade.isDisputed(), "onRejectRequest must not be called once a dispute has started.");

            // update to the latest peer address of our peer if the message is correct
            trade.setTradingPeerNodeAddress(processModel.getTempTradingPeerNodeAddress());
            trade.setBuyersCancelTradeState(BuyerTrade.CancelTradeState.RECEIVED_REJECTED_MSG);
            processModel.removeMailboxMessageAfterProcessing(trade);
            complete();
        } catch (Throwable t) {
            failed(t);
        }
    }
}