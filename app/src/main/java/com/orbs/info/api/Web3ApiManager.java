package com.orbs.info.api;

import android.content.Context;
import android.util.Log;

//import com.ledger.lib.apps.common.ECDSADeviceSignature;
//import com.ledger.lib.transport.LedgerDeviceBLE;
//import com.ledger.lib.transport.LedgerDeviceUSB;
//import com.ledger.lib.utils.Dump;
//import com.orbs.pos.Feature;
//import com.orbs.pos.data.Rewards;
//import com.orbs.pos.util.BLE;
//import com.orbs.pos.util.SharedPreferenceManager;
//import com.orbs.pos.util.USB;
//import com.orbs.pos.util.Util;
//import com.orbs.pos.data.DataManager;
//import com.orbs.pos.data.ErrorCode;
//import com.orbs.pos.data.WalletMode;
//import com.samsung.android.sdk.coldwallet.ScwService;

import com.orbs.info.util.Util;

import org.json.JSONArray;
import org.json.JSONObject;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Bool;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.abi.datatypes.generated.Uint32;
import org.web3j.abi.datatypes.generated.Uint96;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthBlock;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.core.methods.response.EthEstimateGas;
import org.web3j.protocol.core.methods.response.EthGasPrice;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.protocol.core.methods.response.EthLog;
import org.web3j.protocol.http.HttpService;
import org.web3j.utils.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class Web3ApiManager {
    private static final String LOG_TAG = "Web3ApiManager";

    private static final String ROPSTEN_CONTRACT_STAKE = "0x88287444f10709f9531D11e08DCd692deccd1d63";
    private static final String ROPSTEN_CONTRACT_ORBS = "0xed0aa9a4f9e5ae9092994f4b86f6aaa89944939b";

    private static final String CONTRACT_STAKE = API.CONTRACT_STAKE;
    private static final String CONTRACT_DELEGATE = "0xB97178870F39d4389210086E4BcaccACD715c71d"; // V2.5 new delegate contract
    private static final String CONTRACT_ORBS = "0xff56cc6b1e6ded347aa0b7676c85ab0b3d08b0fa";
    private static final String CONTRACT_V2_REWARDS = "0xB5303c22396333D9D27Dc45bDcC8E7Fc502b4B32";
    private static final String CONTRACT_V2_BOOTSTRAP = "0xda7e381544Fc73cad7D9E63C86e561452b9B9E9C";
//    private static final long defaultGasPrice = Util.defaultGasFee;

    public static String ETHERSCAN_URL = "https://etherscan.io/";

    static private Web3ApiManager mInstance;
    private Context mContext;
    private Web3j web3jNode;
    private EthBlock.Block latestBlock;
    private BigInteger latestBlockHeight;
    private BigInteger nextElectionHeight;
    private BigInteger gasPriceGwei = BigInteger.valueOf(100);
    private String SMART_CONTRACT_STAKE;
    private String SMART_CONTRACT_ORBS;
    private String SMART_CONTRACT_DELEGATE;

    private Web3ApiManager() {
        this(null);
    }

    private Web3ApiManager(Context context) {
        mContext = context;
        selectNetwork();
    }

    public static Web3ApiManager getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new Web3ApiManager(context);
        } else {
            mInstance.setContext(context);
        }
        return mInstance;
    }

    public static Web3ApiManager getInstance() {
        if (mInstance == null) {
            mInstance = new Web3ApiManager();
        }

        return mInstance;
    }

    public void selectNetwork() {
        String infura_endpoint_url = API.INFURA_END_POINT;

        ETHERSCAN_URL = "https://etherscan.io/";
        SMART_CONTRACT_STAKE = CONTRACT_STAKE;
        SMART_CONTRACT_ORBS = CONTRACT_ORBS;
        SMART_CONTRACT_DELEGATE = CONTRACT_DELEGATE;

        web3jNode = Web3j.build(new HttpService(infura_endpoint_url));
        Log.i(LOG_TAG, "Web3j node object created.");
    }

    private void setContext(Context context) {
        mContext = context;
    }

    public EthBlock.Block getLatestBlock() {
        EthBlock.Block block = null;
        try {
            EthBlock ethBlock = web3jNode.ethGetBlockByNumber(DefaultBlockParameterName.LATEST, false).sendAsync().get();
            block = ethBlock.getBlock();
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error in getLatestBlock():" + e);
        }

        return block;
    }

    public BigInteger getAllowance(String publicAddress) {
        Function function = getContractFunctionAllowance(publicAddress, SMART_CONTRACT_STAKE);
        Transaction transaction = Transaction.createEthCallTransaction(publicAddress, SMART_CONTRACT_ORBS,
                FunctionEncoder.encode(function));

        Log.d(LOG_TAG, "getAllowance() - func:" + function.getInputParameters());

        try {
            EthCall ethCall = web3jNode.ethCall(transaction, DefaultBlockParameterName.LATEST).sendAsync().get();

            List<Type> decode = FunctionReturnDecoder.decode(ethCall.getResult(),
                    function.getOutputParameters());
            return new BigInteger(decode.get(0).getValue().toString());

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public BigInteger getStakeBalance(String publicAddress) {
        Function function = getContractFunctionGetStakeBalanceOf(publicAddress);
        Transaction transaction = Transaction.createEthCallTransaction(publicAddress, SMART_CONTRACT_STAKE,
                FunctionEncoder.encode(function));

        Log.d(LOG_TAG, "getStakeBalance()");

        try {
            EthCall ethCall = web3jNode.ethCall(transaction, DefaultBlockParameterName.LATEST).sendAsync().get();

            List<Type> decode = FunctionReturnDecoder.decode(ethCall.getResult(),
                    function.getOutputParameters());

            BigInteger ret = new BigInteger(decode.get(0).getValue().toString());

            return ret;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return BigInteger.ZERO;
    }

    public BigInteger getTotalStakedTokens() {
        Function function = getContractFunctionGetTotalStakedTokens();
        Transaction transaction = Transaction.createEthCallTransaction(null, SMART_CONTRACT_STAKE,
                FunctionEncoder.encode(function));

        Log.d(LOG_TAG, "getTotalStakedTokens()");

        try {
            EthCall ethCall = web3jNode.ethCall(transaction, DefaultBlockParameterName.LATEST).sendAsync().get();

            List<Type> decode = FunctionReturnDecoder.decode(ethCall.getResult(),
                    function.getOutputParameters());

            BigInteger ret = new BigInteger(decode.get(0).getValue().toString());
            //DataManager.getInstance(mContext).onGetTotalParticipating(Util.convertToBigDecimal(ret));
            return ret;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return BigInteger.ZERO;
    }

    // get V2 Uncapped delegated staked tokens (ie. team allocation)
    public BigInteger getUncappedDelegatedStake() {
        Function function = getContractFunctionUncappedDelegatedStake("0xffffffffffffffffffffffffffffffffffffffff");
        Transaction transaction = Transaction.createEthCallTransaction(null, CONTRACT_DELEGATE,
                FunctionEncoder.encode(function));

        Log.d(LOG_TAG, "getUncappedDelegatedStake()");

        try {
            EthCall ethCall = web3jNode.ethCall(transaction, DefaultBlockParameterName.LATEST).sendAsync().get();

            List<Type> decode = FunctionReturnDecoder.decode(ethCall.getResult(),
                    function.getOutputParameters());

            return new BigInteger(decode.get(0).getValue().toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return BigInteger.ZERO;
    }

    // get V2 Guardian's rewards percentage settings
    public double getGuardiansRewardSettings(String guardianAddress) {
        Function function = getContractFunctionGuardiansRewardSettings(guardianAddress);
        Transaction transaction = Transaction.createEthCallTransaction(null, CONTRACT_V2_REWARDS,
                FunctionEncoder.encode(function));

        Log.d(LOG_TAG, "getGuardiansRewardSettings()");

        try {
            EthCall ethCall = web3jNode.ethCall(transaction, DefaultBlockParameterName.LATEST).sendAsync().get();

            List<Type> decode = FunctionReturnDecoder.decode(ethCall.getResult(),
                    function.getOutputParameters());

            String percentMille = decode.get(0).getValue().toString();
            boolean overrideDefault = Boolean.parseBoolean(decode.get(1).getValue().toString());

            return overrideDefault ? Double.parseDouble(percentMille + "E-5") : -1; // -1 means using default;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }
// end of read functions

    //Call shutdown to free resource
    public void shutDown() {
        Log.i(LOG_TAG, "Shutting down Etherum Node Connection");
        web3jNode.shutdown();
    }

    private CompletableFuture<EthGetTransactionCount> getNonceRequest(String address) {
        CompletableFuture<EthGetTransactionCount> nonceRequest;
        nonceRequest = web3jNode.ethGetTransactionCount(address, DefaultBlockParameterName.PENDING).sendAsync();
        return nonceRequest;
    }

    // contract functions definition (input, output)
    private Function getContractFunctionDelegate(String guardianAddress) {
        return new Function("delegate",
                Arrays.asList(new Address(guardianAddress)),
                Collections.emptyList());
    }

    private Function getContractFunctionTransfer(String address, BigInteger amountValue) {
        return new Function("transfer",
                Arrays.asList(new Address(address), new Uint256(amountValue)),
                Collections.emptyList());
    }

    private Function getContractFunctionApprove(String address, BigInteger amountValue) {
        return new Function("approve",
                Arrays.asList(new Address(address), new Uint256(amountValue)),
                Collections.emptyList());
    }

    private Function getContractFunctionBalanceOf(String address) {
        return new Function("balanceOf",
                Arrays.asList(new Address(address)),                // input parameter
                Arrays.asList(new TypeReference<Uint256>() {}));    // output parameter
    }

    private Function getContractFunctionAllowance(String owner, String spender) {
        return new Function("allowance",
                Arrays.asList(new Address(owner), new Address(spender)),                // input parameter
                Arrays.asList(new TypeReference<Uint256>() {}));    // output parameter
    }

    private Function getContractFunctionStake(BigInteger amountValue) {
        // contract function: stake(uint256 _amount)
        return new Function("stake",
                Arrays.asList(new Uint256(amountValue)),
                Collections.emptyList());
    }

    private Function getContractFunctionUnstake(BigInteger amountValue) {
        // function unstake(uint256 _amount)
        return new Function("unstake",
                Arrays.asList(new Uint256(amountValue)),
                Collections.emptyList());
    }

    private Function getContractFunctionWithdraw() {
        // function withdraw() external
        return new Function("withdraw",
                Collections.emptyList(),
                Collections.emptyList());
    }

    private Function getContractFunctionRestake() {
        // function restake()
        return new Function("restake",
                Collections.emptyList(),
                Collections.emptyList());
    }

    private Function getContractFunctionClaimStakingRewards(String address) {
        // function restake()
        return new Function("claimStakingRewards",
                Arrays.asList(new Address(address)),                // input parameter
                Collections.emptyList());
    }

    private Function getContractFunctionWithdrawBootstrapFunds(String address) {
        // function restake()
        return new Function("withdrawBootstrapFunds",
                Arrays.asList(new Address(address)),                // input parameter
                Collections.emptyList());
    }

    private Function getContractFunctionWithdrawFees(String address) {
        // function restake()
        return new Function("withdrawFees",
                Arrays.asList(new Address(address)),                // input parameter
                Collections.emptyList());
    }

    private Function getContractFunctionGetStakeBalanceOf(String address) {
        //    function getStakeBalanceOf(address _stakeOwner) returns (uint256) {
        return new Function("getStakeBalanceOf",
                Arrays.asList(new Address(address)),                // input parameter
                Arrays.asList(new TypeReference<Uint256>() {}));    // output parameter
    }

    private Function getContractFunctionGetUnstakeStatus(String address) {
        //function getUnstakeStatus(address _stakeOwner) returns (uint256 cooldownAmount, uint256 cooldownEndTime)
        return new Function("getUnstakeStatus",
                Arrays.asList(new Address(address)),                // input parameter
                Arrays.asList(new TypeReference<Uint256>() {}, new TypeReference<Uint256>() {}));    // output parameter
    }

    private Function getContractFunctionGetTotalStakedTokens() {
        // function getTotalStakedTokens() external view returns (uint256);
        return new Function("getTotalStakedTokens",
                Collections.emptyList(),                // input parameter
                Arrays.asList(new TypeReference<Uint256>() {}));    // output parameter
    }

    private Function getContractFunctionGetDelegationInfo(String address) {
//        function getDelegationInfo(address addr) external view returns (address delegation, uint256 delegatorStake);
        return new Function("getDelegationInfo",
                Arrays.asList(new Address(address)),                // input parameter
                Arrays.asList(new TypeReference<Address>() {},
                        new TypeReference<Uint256>() {}));    // output parameter
    }

    private Function getContractFunctionUncappedDelegatedStake(String address) {
//        function uncappedDelegatedStake(address addr) external view returns (uint256 delegatorStake);
        return new Function("uncappedDelegatedStake",
                Arrays.asList(new Address(address)),                // input parameter
                Arrays.asList(new TypeReference<Uint256>() {}));    // output parameter
    }

    private Function getContractFunctionGetStakingRewardsBalance(String address) {
//        function getDelegatorStakingRewardsData(address delegator) external view returns (
//                uint256 delegatorStakingRewardsBalance   ,
//                uint256 guardianStakingRewardsBalance,
        return new Function("getStakingRewardsBalance",
                Arrays.asList(new Address(address)),                // input parameter
                Arrays.asList(new TypeReference<Uint256>() {},
                        new TypeReference<Uint256>() {}));    // output parameter
    }

    private Function getContractFunctionDelegatorsStakingRewards (String address) {
//        function delegatorsStakingRewards(address delegator) external view returns (
//        balance uint96, lastDelegatorRewardsPerToken uint96, claimed uint96
        return new Function("delegatorsStakingRewards",
                Arrays.asList(new Address(address)),                // input parameter
                Arrays.asList(new TypeReference<Uint96>() {},
                        new TypeReference<Uint96>() {},
                        new TypeReference<Uint96>() {}));    // output parameter
    }

    private Function getContractFunctionGuardiansStakingRewards (String address) {
//        function guardiansStakingRewards(address delegator) external view returns (
//          delegatorRewardsPerToken uint96, lastStakingRewardsPerWeight uint96, balance uint96, claimed uint96
        return new Function("guardiansStakingRewards",
                Arrays.asList(new Address(address)),                // input parameter
                Arrays.asList(new TypeReference<Uint96>() {},
                        new TypeReference<Uint96>() {},
                        new TypeReference<Uint96>() {},
                        new TypeReference<Uint96>() {}));    // output parameter
    }

    private Function getContractFunctionEstimateFutureRewards (String address, BigInteger duration) {
//        function estimateFutureRewards(address addr, uint256 duration) external view returns (
//                uint256 estimatedDelegatorStakingRewards,
//                uint256 estimatedGuardianStakingRewards
//        );
        return new Function("estimateFutureRewards",
                Arrays.asList(new Address(address), new Uint256(duration)), // input parameter
                Arrays.asList(new TypeReference<Uint256>() {},
                        new TypeReference<Uint256>() {}));    // output parameter
    }

    private Function getContractFunctionGetFeesAndBootstrapBalance (String address) {
//        function getFeesAndBootstrapBalance(address guardian) external view returns (
//                uint256 feeBalance,
//                uint256 bootstrapBalance
//        );
        return new Function("getFeesAndBootstrapBalance",
                Arrays.asList(new Address(address)), // input parameter
                Arrays.asList(new TypeReference<Uint256>() {},
                        new TypeReference<Uint256>() {}));    // output parameter
    }

    private Function getContractFunctionGuardiansRewardSettings(String address) {
//        function uncappedDelegatedStake(address addr) external view returns (uint256 delegatorStake);
        return new Function("guardiansRewardSettings",
                Arrays.asList(new Address(address)),                // input parameter
                Arrays.asList(new TypeReference<Uint32>() {}, new TypeReference<Bool>() {}));    // output parameter
    }

    // get current average gas price from the ETH network
    public void getGasPrice() {
        CompletableFuture<EthGasPrice> ethGetBalanceCompletableFuture;
        ethGetBalanceCompletableFuture = web3jNode.ethGasPrice().sendAsync();
        ethGetBalanceCompletableFuture.thenApply(ethGasPrice -> {
            // Balance will be set once the data is fetched
            BigDecimal priceInWei = new BigDecimal(ethGasPrice.getGasPrice());
            BigDecimal priceInGwei = priceInWei.divide(new BigDecimal(BigInteger.TEN.pow(9)));
            this.gasPriceGwei = priceInGwei.toBigInteger();
            String fetchedPrice= priceInWei.toString();
            Log.i(LOG_TAG, "Fetched price: " + fetchedPrice);
//            getGasPriceByUserSetting();
//            DataManager.getInstance(mContext).onGetGasPrice((Integer.valueOf(gasPriceGwei.toString())));
            return ethGasPrice;       //dummy return
        });
    }

    private BigInteger getTransactionGasLimit(Transaction transaction) {
        try {
            EthEstimateGas ethEstimateGas = web3jNode.ethEstimateGas(transaction).send();
            if (ethEstimateGas.hasError()){
                throw new RuntimeException(ethEstimateGas.getError().getMessage());
            }
            Log.i(LOG_TAG, "getTransactionGasLimit() - returned value = " + ethEstimateGas.getAmountUsed().intValue());
            return ethEstimateGas.getAmountUsed().add(BigInteger.valueOf(35000)); // add 35,000 extra value
        } catch (IOException e) {
            //throw new RuntimeException("net error");
            return BigInteger.ZERO;
        }
    }

    public JSONObject getContractEventLogs(BigInteger from, String contractAddress, String topic0) throws IOException{
        JSONObject retJson = new JSONObject();
        EthFilter ethFilter = new EthFilter(DefaultBlockParameter.valueOf(from),
                                    DefaultBlockParameterName.LATEST, contractAddress);
        ethFilter.addSingleTopic(topic0);

        try {
            EthLog ethLog = web3jNode.ethGetLogs(ethFilter).sendAsync().get();

            retJson = Util.convertWeb3Log2Json(ethLog);

            Log.d(LOG_TAG, "getContractEventLogs");

        } catch (Exception e) {
            e.printStackTrace();
        }

        return retJson;
    }
}
