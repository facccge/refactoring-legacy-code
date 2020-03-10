package cn.xpbootcamp.legacy_code;

import cn.xpbootcamp.legacy_code.service.WalletService;
import cn.xpbootcamp.legacy_code.service.WalletServiceImpl;
import cn.xpbootcamp.legacy_code.utils.RedisDistributedLock;
import org.junit.jupiter.api.Test;

import javax.transaction.InvalidTransactionException;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.doReturn;

class WalletTransactionTest {

    @Test
    void executeTestThrowsExceptionWhenBuyerIdIsNull() throws InvalidTransactionException {
        WalletTransaction walletTransaction = new WalletTransaction("t_000001", null, 999001L, 100001L, "o_000001", 100d);
        assertThrows(InvalidTransactionException.class, () -> {
            walletTransaction.execute();
        });
    }

    @Test
    void executeTestThrowsExceptionWhenSellerIdIsNull() throws InvalidTransactionException {
        WalletTransaction walletTransaction = new WalletTransaction("t_000001", 666001L, null, 100001L, "o_000001", 100d);
        assertThrows(InvalidTransactionException.class, () -> {
            walletTransaction.execute();
        });
    }

    @Test
    void executeTestThrowsExceptionWhenAmountLessThan0() throws InvalidTransactionException {
        WalletTransaction walletTransaction = new WalletTransaction("t_000001", 666001L, 999001L, 100001L, "o_000001", -10d);
        assertThrows(InvalidTransactionException.class, () -> {
            walletTransaction.execute();
        });
    }

    @Test
    void executeTestReturnTrueWhenExecuteSuccess() throws InvalidTransactionException {
        RedisDistributedLock redisDistributedLock = mock(RedisDistributedLock.class);
        when(redisDistributedLock.lock("t_000001")).thenReturn(true);

        WalletService walletService = mock(WalletServiceImpl.class);
        when(walletService.moveMoney("t_000001", 666001L, 999001L, 100d)).thenReturn(UUID.randomUUID().toString() + "t_000001");

        WalletTransaction walletTransaction = spy(new WalletTransaction("t_000001", 666001L, 999001L, 100001L, "o_000001", 100d));
        doReturn(redisDistributedLock).when(walletTransaction).getRedisDistributedLockinstance();
        doReturn(walletService).when(walletTransaction).getWalletService();

        assertTrue(walletTransaction.execute());
    }

    @Test
    void executeTestReturnTrueWhenLockedFailed() throws InvalidTransactionException {
        RedisDistributedLock redisDistributedLock = mock(RedisDistributedLock.class);
        when(redisDistributedLock.lock("t_000001")).thenReturn(false);

        WalletService walletService = mock(WalletServiceImpl.class);
        when(walletService.moveMoney("t_000001", 666001L, 999001L, 100d)).thenReturn(UUID.randomUUID().toString() + "t_000001");

        WalletTransaction walletTransaction = spy(new WalletTransaction("t_000001", 666001L, 999001L, 100001L, "o_000001", 100d));
        doReturn(redisDistributedLock).when(walletTransaction).getRedisDistributedLockinstance();
        doReturn(walletService).when(walletTransaction).getWalletService();

        assertFalse(walletTransaction.execute());
    }

    @Test
    void executeTestReturnTrueWhenWalletTransactionIdIsNull() throws InvalidTransactionException {
        RedisDistributedLock redisDistributedLock = mock(RedisDistributedLock.class);
        when(redisDistributedLock.lock("t_000001")).thenReturn(true);

        WalletService walletService = mock(WalletServiceImpl.class);
        when(walletService.moveMoney("t_000001", 666001L, 999001L, 100d))
                .thenReturn(null);

        WalletTransaction walletTransaction = spy(new WalletTransaction("t_000001", 666001L, 999001L, 100001L, "o_000001", 100d));
        doReturn(redisDistributedLock).when(walletTransaction).getRedisDistributedLockinstance();
        doReturn(walletService).when(walletTransaction).getWalletService();

        assertFalse(walletTransaction.execute());
    }

    @Test
    void executeTestReturnTrueWhenExpired() throws InvalidTransactionException {
        RedisDistributedLock redisDistributedLock = mock(RedisDistributedLock.class);
        when(redisDistributedLock.lock("t_000001")).thenReturn(true);

        WalletService walletService = mock(WalletServiceImpl.class);
        when(walletService.moveMoney("t_000001", 666001L, 999001L, 100d)).thenReturn(UUID.randomUUID().toString() + "t_000001");

        WalletTransaction walletTransaction = spy(new WalletTransaction("t_000001", 666001L, 999001L, 100001L, "o_000001", 100d));
        doReturn(redisDistributedLock).when(walletTransaction).getRedisDistributedLockinstance();
        doReturn(walletService).when(walletTransaction).getWalletService();
        doReturn(true).when(walletTransaction).isExpired();

        assertFalse(walletTransaction.execute());
    }
}
