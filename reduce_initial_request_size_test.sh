#!/bin/sh

install()
{
	mkdir -p installdir/$2
	cp bisq-$1 installdir/$2/
	cp -r lib installdir/$2/
}

declare -A cmd
cmd['bitcoind']="bitcoind -regtest -prune=0 -txindex=1 -peerbloomfilters=1 -server -rpcuser=bisqdao -rpcpassword=bsq -datadir=.localnet/bitcoind -blocknotify='.localnet/bitcoind/blocknotify %s'"
cmd['alice']="installdir/alice/bisq-desktop --baseCurrencyNetwork=BTC_REGTEST --useLocalhostForP2P=true --useDevPrivilegeKeys=true --nodePort=5555 --fullDaoNode=true --rpcUser=bisqdao --rpcPassword=bsq --rpcBlockNotificationPort=5122 --genesisBlockHeight=111 --genesisTxId=30af0050040befd8af25068cc697e418e09c2d8ebd8d411d2240591b9ec203cf --appDataDir=.localnet/alice --appName=Alice"
cmd['bob']="installdir/bob/bisq-desktop --baseCurrencyNetwork=BTC_REGTEST --useLocalhostForP2P=true --useDevPrivilegeKeys=true --nodePort=6666 --appDataDir=.localnet/bob --appName=Bob"
cmd['mediator']="installdir/mediator/bisq-desktop --baseCurrencyNetwork=BTC_REGTEST --useLocalhostForP2P=true --useDevPrivilegeKeys=true --nodePort=4444 --appDataDir=.localnet/mediator --appName=Mediator"
cmd['seednode']="installdir/seednode/bisq-seednode --baseCurrencyNetwork=BTC_REGTEST --useLocalhostForP2P=true --useDevPrivilegeKeys=true --fullDaoNode=true --rpcUser=bisqdao --rpcPassword=bsq --rpcBlockNotificationPort=5120 --nodePort=2002 --userDataDir=.localnet --appName=seednode"
cmd['seednode2']="installdir/seednode2/bisq-seednode --baseCurrencyNetwork=BTC_REGTEST --useLocalhostForP2P=true --useDevPrivilegeKeys=true --fullDaoNode=true --rpcUser=bisqdao --rpcPassword=bsq --rpcBlockNotificationPort=5121 --nodePort=3002 --userDataDir=.localnet --appName=seednode2"

run()
{
	# create a new screen session named 'localnet'
	screen -dmS localnet
	# deploy each node in its own named screen window
	for target in \
			bitcoind \
			seednode \
			seednode2 \
			alice \
			bob \
			mediator; do \
#		echo "$target: ${cmd[$target]}"
		screen -S localnet -X screen -t $target; \
		screen -S localnet -p $target -X stuff "${cmd[$target]}\n"; \
		sleep 3
	done;
	# give bitcoind rpc server time to start
	sleep 5
	# generate a block to ensure Bisq nodes get dao-synced
	make block
}

# - shut down anything again
staap()
{
	# kill all Bitcoind and Bisq nodes running in screen windows
	screen -S localnet -X at "#" stuff "^C"
	# quit all screen windows which results in killing the session
	screen -S localnet -X at "#" kill
	screen -wipe
}

check()
{
	# gather data
	for target in \
		seednode \
		seednode2 \
		alice \
		bob \
		mediator; do \
		echo "$target:" >> result.log; \
		grep -Eo ": (Sending .*Request|Received .*Response) with [0-9\.]* kB" .localnet/$target/bisq.log >> result.log; \
		rm .localnet/$target/bisq.log; \
	done;
}

# clean everything for a fresh test run
rm -rf .localnet
rm -rf installdir
staap

# deploy configuration files and start bitcoind
make localnet
for target in \
	seednode \
	seednode2 \
	alice \
	bob \
	mediator; do \
	mkdir -p .localnet/$target/btc_regtest/db/; \
	cp PreferencesPayload .localnet/$target/btc_regtest/db/; \
done;


# - borrow mainnet data stores to better illustrate stuff
cd p2p/src/main/resources/
cp TradeStatistics2Store_BTC_MAINNET TradeStatistics2Store_BTC_REGTEST
cp AccountAgeWitnessStore_BTC_MAINNET AccountAgeWitnessStore_BTC_REGTEST
cp SignedWitnessStore_BTC_MAINNET SignedWitnessStore_BTC_REGTEST
cd -

# start with 1.3.2 setup
# - get sources for 1.3.2
git checkout v1.3.2 -f

# - build initial binaries and file structure
./gradlew :seednode:build
./gradlew :desktop:build

# - install binaries
install seednode seednode
install seednode seednode2
install desktop alice
install desktop bob
install desktop mediator

# - fire up all of it
run

# - setup mediator/refund agent
sleep 5
read -p "Wrap up first test case?"

# - shut down everything
staap

echo "##### Sanity check ###########################################" > result.log
check

# upgrade to PR
git checkout -f reduce_initial_request_size

# create release data stores
cd p2p/src/main/resources/
cp TradeStatistics2Store_BTC_REGTEST TradeStatistics2Store_1.3.2_BTC_REGTEST
cp AccountAgeWitnessStore_BTC_REGTEST AccountAgeWitnessStore_1.3.2_BTC_REGTEST
cp SignedWitnessStore_BTC_REGTEST SignedWitnessStore_1.3.2_BTC_REGTEST
cd -
./gradlew :seednode:build

# install seednode binaries
install seednode seednode

# fire up all of it
run

sleep 5
read -p "Wrap up second test case?"

# shut down anything again
staap

echo "##### After upgrading one seednode ###########################" >> result.log
check

## install client binaries
install desktop alice

## fire up all of it
run

read -p "Wrap up third test case?"

# shut down anything again
staap

echo "##### After upgrading one client ###########################" >> result.log
check
