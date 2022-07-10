package org.easyfarmer.chia;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.easyfarmer.chia.cmd.NftWallet;
import org.easyfarmer.chia.util.ChiaUtils;
import org.easyfarmer.chia.util.Constant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.List;

/**
 * @author liyifeng
 * @version 1.0
 * @date 2022/7/9 10:32 下午
 */
public class CheckWallet2Transfer implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(CheckWallet2Transfer.class);

    private static boolean monitor = false;
    private static String targetWalletAddress;
    private static Long transferFee; //转账手续费

    public static void stopMonitor() {
        targetWalletAddress = null;
        monitor = false;
    }

    public static void startMonitor(String walletAddress, Long fee) {
        monitor = true;
        targetWalletAddress = walletAddress;
        if (fee == null) {
            transferFee = Constant.DEFAULT_TRANSFER_FEE;
        } else {
            transferFee = fee;

        }
    }

    @Override
    public void run() {
        while (true) {
            try {
                if (!monitor || targetWalletAddress == null || targetWalletAddress.trim().length() != 62) {
                    continue;
                }
                JSONObject walletRespJson = ChiaUtils.get_wallets();
                Integer walletId = getChiaWalletId(walletRespJson);
                if (walletId == null) {
                    //logger.warn("获取钱包账户ID失败:{}", json);
                    return;
                }

                JSONObject walletBalanceJson = ChiaUtils.get_wallet_balance(walletId);
                JSONObject balanceDataJson = null;
                String fingerprint = null;

                if (chiaRpcSuccess(walletBalanceJson) && walletBalanceJson.containsKey("wallet_balance")) {
                    balanceDataJson = walletBalanceJson.getJSONObject("wallet_balance");
                    fingerprint = balanceDataJson.getString("fingerprint");
                } else {
                    continue;
                }

                // 检查钱包余额
                checkWalletBalance(balanceDataJson, fingerprint);

                // 检查待认领奖励
                //checkClaim(fingerprint);

            } catch (Exception e) {
                //logger.error("报错", e);
            } finally {
                try {
                    Thread.sleep(10000L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // 检查待认领待奖励
    private void checkClaim(String fingerprint) {
        try {

            List<NftWallet> nftWalletList = ChiaUtils.plotNftShow(fingerprint);

            if (nftWalletList != null && nftWalletList.size() > 0) {
                for (NftWallet nftWallet : nftWalletList) {
                    if (nftWallet.getClaimableBalanceMojo() > 0) {
                        logger.info("认领奖励,walletId:{},金额:{},launchId:{}", nftWallet.getWalletId(), nftWallet.getClaimableBalanceXch(), nftWallet.getLauncherId());
                        String claimResp = ChiaUtils.plotNftClaim(fingerprint, nftWallet.getWalletId());
                        logger.info("认领结果:{}", claimResp);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void checkWalletBalance(JSONObject balanceDataJson, String fingerprint) {
        try {
            logger.info("当前钱包余额情况：{}", balanceDataJson);
            Long balance = balanceDataJson.getLong("confirmed_wallet_balance");

            if (balance > 0) {//需要转账

                //钱包账户有余额
                String fee = null;
                if (transferFee > 0) { //有手续费
                    fee = ChiaUtils.mojo2xch(new BigDecimal(transferFee));
                    balance -= transferFee;
                }
                if (balance <= 0) {
                    return;
                }

                String amt = ChiaUtils.mojo2xch(new BigDecimal(balance));
                APP.app.addLog(String.format("自动转账金额：%s，手续费：%s，目标账户：%s", amt, fee, targetWalletAddress));
                List<String> lines = ChiaUtils.transfer(fingerprint, targetWalletAddress, amt, fee);
                APP.app.addLog("转账结果：");
                for (String line : lines) {
                    APP.app.addLog(line);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private Integer getChiaWalletId(JSONObject json) {
        // 执行成功
        if (chiaRpcSuccess(json) && json.containsKey("wallets")) {
            JSONArray walletArray = json.getJSONArray("wallets");
            if (walletArray != null) {
                for (Object o : walletArray) {
                    JSONObject j = (JSONObject) o;
                    if (j.getInteger("type") != null && j.getInteger("type").intValue() == 0) {
                        return j.getInteger("id");
                    }
                }
            }
        }

        return null;
    }

    private boolean chiaRpcSuccess(JSONObject json) {
        return json != null && json.containsKey("success") && json.getBoolean("success");
    }
    // 获得钱包列表
    //            chia rpc wallet get_wallets
    //            {
    //                "success": true,
    //                    "wallets": [
    //                {
    //                    "data": "",
    //                        "id": 1,
    //                        "name": "Chia Wallet",
    //                        "type": 0
    //                },
    //                {
    //                    "data": "",
    //                        "id": 2,
    //                        "name": "Pool wallet",
    //                        "type": 9
    //                },
    //                {
    //                    "data": "",
    //                        "id": 3,
    //                        "name": "Pool wallet",
    //                        "type": 9
    //                },
    //                {
    //                    "data": "",
    //                        "id": 4,
    //                        "name": "Pool wallet",
    //                        "type": 9
    //                },
    //                {
    //                    "data": "509deafe3cd8bbfbb9ccce1d930e3d7b57b40c964fa33379b18d628175eb7a8f00",
    //                        "id": 5,
    //                        "name": "Chia Holiday 2021 Token",
    //                        "type": 6
    //                },
    //                {
    //                    "data": "",
    //                        "id": 6,
    //                        "name": "Pool wallet",
    //                        "type": 9
    //                }
    //]
    //            }
}
