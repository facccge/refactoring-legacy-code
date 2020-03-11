package cn.xpbootcamp.legacy_code;

import cn.xpbootcamp.legacy_code.enums.STATUS;
import cn.xpbootcamp.legacy_code.service.WalletService;
import cn.xpbootcamp.legacy_code.service.WalletServiceImpl;
import cn.xpbootcamp.legacy_code.utils.IdGenerator;
import cn.xpbootcamp.legacy_code.utils.RedisDistributedLock;

import javax.transaction.InvalidTransactionException;

public class WalletTransaction {
    private static final int TWENTY_DAYS_TIMESTAMP = 1728000000;
    public static final String PREFIX_OF_TRANSACTION_ID = "t_";

    private String id;
    private Long buyerId;
    private Long sellerId;
    private Long productId;
    private String orderId;
    private Long createdTimestamp;
    private Double amount;
    private STATUS status;
    private String walletTransactionId;


    public WalletTransaction(String preAssignedId, Long buyerId, Long sellerId, Long productId, String orderId, Double amount) {
        this.id = generateId(preAssignedId);
        this.buyerId = buyerId;
        this.sellerId = sellerId;
        this.productId = productId;
        this.orderId = orderId;
        this.amount = amount;
        this.status = STATUS.TO_BE_EXECUTED;
        this.createdTimestamp = System.currentTimeMillis();
    }

    String generateId(String preAssignedId) {
        if (isValidPreAssignedId(preAssignedId)) {
            return preAssignedId.startsWith(PREFIX_OF_TRANSACTION_ID) ? preAssignedId : PREFIX_OF_TRANSACTION_ID + preAssignedId;
        }
        return PREFIX_OF_TRANSACTION_ID + IdGenerator.generateTransactionId();
    }

    private boolean isValidPreAssignedId(String preAssignedId) {
        return preAssignedId != null && !preAssignedId.isEmpty();
    }

    public boolean execute() throws InvalidTransactionException {
        if (isInvalidTransaction()) {
            throw new InvalidTransactionException("This is an invalid transaction");
        }

        if (isExecuted()) {
            return true;
        }

        RedisDistributedLock redisDistributedLock = getRedisDistributedLockinstance();

        boolean isLocked = false;
        try {
            isLocked = redisDistributedLock.lock(id);

            if (!isLocked) {
                return false;
            }

            if (isExpired()) {
                this.status = STATUS.EXPIRED;
                return false;
            }

            WalletService walletService = getWalletService();
            String walletTransactionId = walletService.moveMoney(id, buyerId, sellerId, amount);
            if (isValidWalletTransactionId(walletTransactionId)) {
                this.walletTransactionId = walletTransactionId;
                this.status = STATUS.EXECUTED;
                return true;
            } else {
                this.status = STATUS.FAILED;
                return false;
            }
        } finally {
            if (isLocked) {
                redisDistributedLock.unlock(id);
            }
        }
    }

    boolean isValidWalletTransactionId(String walletTransactionId) {
        return walletTransactionId != null;
    }

    boolean isExecuted() {
        return status == STATUS.EXECUTED;
    }

    boolean isInvalidTransaction() {
        return buyerId == null || (sellerId == null || amount < 0.0);
    }

    boolean isExpired() {
        return System.currentTimeMillis() - this.createdTimestamp > TWENTY_DAYS_TIMESTAMP;
    }

    WalletServiceImpl getWalletService() {
        return new WalletServiceImpl();
    }

    RedisDistributedLock getRedisDistributedLockinstance() {
        return RedisDistributedLock.getSingletonInstance();
    }

}
