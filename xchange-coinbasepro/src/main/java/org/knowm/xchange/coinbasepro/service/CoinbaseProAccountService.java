package org.knowm.xchange.coinbasepro.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.knowm.xchange.Exchange;
import org.knowm.xchange.coinbasepro.CoinbaseProAdapters;
import org.knowm.xchange.coinbasepro.dto.CoinbaseProTransfer;
import org.knowm.xchange.coinbasepro.dto.CoinbaseProTransfers;
import org.knowm.xchange.coinbasepro.dto.account.CoinbaseProFee;
import org.knowm.xchange.coinbasepro.dto.account.CoinbaseProWithdrawCryptoResponse;
import org.knowm.xchange.coinbasepro.dto.trade.CoinbaseProAccount;
import org.knowm.xchange.coinbasepro.dto.trade.CoinbaseProAccountAddress;
import org.knowm.xchange.coinbasepro.dto.trade.CoinbaseProSendMoneyResponse;
import org.knowm.xchange.coinbasepro.dto.trade.CoinbaseProTradeHistoryParams;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.account.AccountInfo;
import org.knowm.xchange.dto.account.AddressWithTag;
import org.knowm.xchange.dto.account.Fee;
import org.knowm.xchange.dto.account.FundingRecord;
import org.knowm.xchange.exceptions.ExchangeException;
import org.knowm.xchange.service.account.AccountService;
import org.knowm.xchange.service.trade.params.DefaultWithdrawFundsParams;
import org.knowm.xchange.service.trade.params.HistoryParamsFundingType;
import org.knowm.xchange.service.trade.params.TradeHistoryParams;
import org.knowm.xchange.service.trade.params.WithdrawFundsParams;

public class CoinbaseProAccountService extends CoinbaseProAccountServiceRaw
    implements AccountService {

  public CoinbaseProAccountService(Exchange exchange) {

    super(exchange);
  }

  @Override
  public AccountInfo getAccountInfo() throws IOException {
    return new AccountInfo(CoinbaseProAdapters.adaptAccountInfo(getCoinbaseProAccountInfo()));
  }

  @Override
  public Map<CurrencyPair, Fee> getDynamicTradingFees() throws IOException {
    CurrencyPair[] pairs = exchange.getExchangeSymbols().toArray(new CurrencyPair[0]);
    return getDynamicTradingFeeForPairs(pairs);
  }

  @Override
  public Map<CurrencyPair, Fee> getDynamicTradingFeeForPairs(CurrencyPair[] currencyPairs) throws IOException {
    CoinbaseProFee fees = getCoinbaseProFees();
    Map<CurrencyPair, Fee> tradingFees = new HashMap<>();
    for (CurrencyPair cp : currencyPairs) {
      tradingFees.put(cp, Fee.newDecimalFee(fees.getMakerRate(), fees.getTakerRate()));
    }
    return tradingFees;
  }

  @Override
  public String withdrawFunds(Currency currency, BigDecimal amount, String address)
      throws IOException {
    return withdrawFunds(new DefaultWithdrawFundsParams(address, currency, amount));
  }

  @Override
  public String withdrawFunds(Currency currency, BigDecimal amount, AddressWithTag address)
      throws IOException {
    return withdrawFunds(new DefaultWithdrawFundsParams(address, currency, amount));
  }

  @Override
  public String withdrawFunds(WithdrawFundsParams params) throws IOException {
    if (params instanceof DefaultWithdrawFundsParams) {
      DefaultWithdrawFundsParams defaultParams = (DefaultWithdrawFundsParams) params;
      CoinbaseProWithdrawCryptoResponse response =
          withdrawCrypto(
              defaultParams.getAddress(),
              defaultParams.getAmount(),
              defaultParams.getCurrency(),
              defaultParams.getAddressTag(),
              defaultParams.getAddressTag() == null);
      return response.id;
    }

    throw new IllegalStateException("Don't know how to withdraw: " + params);
  }

  public String moveFunds(Currency currency, String address, BigDecimal amount) throws IOException {
    org.knowm.xchange.coinbasepro.dto.account.CoinbaseProAccount[] accounts =
        getCoinbaseProAccountInfo();
    String accountId = null;
    for (org.knowm.xchange.coinbasepro.dto.account.CoinbaseProAccount account : accounts) {
      if (currency.getCurrencyCode().equals(account.getCurrency())) {
        accountId = account.getId();
      }
    }

    if (accountId == null) {
      throw new ExchangeException(
          "Cannot determine account id for currency " + currency.getCurrencyCode());
    }

    CoinbaseProSendMoneyResponse response = sendMoney(accountId, address, amount, currency);
    if (response.getData() != null) {
      return response.getData().getId();
    }

    return null;
  }

  private CoinbaseProAccountAddress accountAddress(Currency currency, String... args)
      throws IOException {
    CoinbaseProAccount[] coinbaseAccounts = getCoinbaseAccounts();
    CoinbaseProAccount depositAccount = null;

    for (CoinbaseProAccount account : coinbaseAccounts) {
      Currency accountCurrency = Currency.getInstance(account.getCurrency());
      if (account.isActive()
          && account.getType().equals("wallet")
          && accountCurrency.equals(currency)) {
        depositAccount = account;
        break;
      }
    }

    CoinbaseProAccountAddress accountAddress = getCoinbaseAccountAddress(depositAccount.getId());
    return accountAddress;
  }

  @Deprecated
  @Override
  public String requestDepositAddress(Currency currency, String... args) throws IOException {
    CoinbaseProAccountAddress depositAddress = accountAddress(currency, args);
    return depositAddress.getAddress();
  }

  @Override
  public AddressWithTag requestDepositAddressData(Currency currency, String... args)
      throws IOException {
    CoinbaseProAccountAddress depositAddress = accountAddress(currency, args);
    return new AddressWithTag(depositAddress.getAddress(), depositAddress.getDestinationTag());
  }

  @Override
  public TradeHistoryParams createFundingHistoryParams() {
    return new CoinbaseProTradeHistoryParams();
  }

  @Override
  /*
   * Warning - this method makes several API calls. The reason is that the paging functionality
   * isn't implemented properly yet.
   *
   * <p>It honours TradeHistoryParamCurrency for filtering to a single ccy.
   */
  public List<FundingRecord> getFundingHistory(TradeHistoryParams params) throws IOException {

    String fundingRecordType;
    if (params instanceof HistoryParamsFundingType
        && ((HistoryParamsFundingType) params).getType() != null) {
      FundingRecord.Type type = ((HistoryParamsFundingType) params).getType();
      fundingRecordType = type == FundingRecord.Type.WITHDRAWAL ? "withdraw" : "deposit";
    } else {
      throw new ExchangeException(
          "Type 'deposit' or 'withdraw' must be supplied using FundingRecord.Type");
    }

    int maxPageSize = 100;

    List<FundingRecord> fundingHistory = new ArrayList<>();

    Map<String, String> accountToCurrencyMap =
        Stream.of(getCoinbaseProAccountInfo())
            .collect(
                Collectors.toMap(
                    org.knowm.xchange.coinbasepro.dto.account.CoinbaseProAccount::getId,
                    org.knowm.xchange.coinbasepro.dto.account.CoinbaseProAccount::getCurrency));

    String createdAt = null; // use to get next page
    while (true) {
      String createdAtFinal = createdAt;
      CoinbaseProTransfers transfers =
          transfers(fundingRecordType, null, null, createdAtFinal, maxPageSize);

      for (CoinbaseProTransfer coinbaseProTransfer : transfers) {
        Currency currency =
            Currency.getInstance(accountToCurrencyMap.get(coinbaseProTransfer.getAccountId()));
        fundingHistory.add(CoinbaseProAdapters.adaptFundingRecord(currency, coinbaseProTransfer));
      }

      if (transfers.size() < maxPageSize) {
        break;
      }

      createdAt = transfers.getHeader("Cb-After");
    }

    return fundingHistory;
  }

  public static class CoinbaseProMoveFundsParams implements WithdrawFundsParams {
    public final Currency currency;
    public final BigDecimal amount;
    public final String address;

    public CoinbaseProMoveFundsParams(Currency currency, BigDecimal amount, String address) {
      this.currency = currency;
      this.amount = amount;
      this.address = address;
    }
  }
}
