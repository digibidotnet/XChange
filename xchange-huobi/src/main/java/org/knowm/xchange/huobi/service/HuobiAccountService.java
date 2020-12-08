package org.knowm.xchange.huobi.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;

import org.apache.commons.lang3.StringUtils;
import org.knowm.xchange.Exchange;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.account.AccountInfo;
import org.knowm.xchange.dto.account.AddressWithTag;
import org.knowm.xchange.dto.account.Fee;
import org.knowm.xchange.dto.account.FundingRecord;
import org.knowm.xchange.exceptions.ExchangeException;
import org.knowm.xchange.huobi.HuobiAdapters;
import org.knowm.xchange.huobi.HuobiUtils;
import org.knowm.xchange.huobi.dto.account.HuobiAccount;
import org.knowm.xchange.huobi.dto.account.HuobiDepositAddress;
import org.knowm.xchange.huobi.dto.account.HuobiTransactFeeRate;
import org.knowm.xchange.service.account.AccountService;
import org.knowm.xchange.service.trade.params.DefaultWithdrawFundsParams;
import org.knowm.xchange.service.trade.params.HistoryParamsFundingType;
import org.knowm.xchange.service.trade.params.TradeHistoryParamCurrency;
import org.knowm.xchange.service.trade.params.TradeHistoryParams;
import org.knowm.xchange.service.trade.params.TradeHistoryParamsIdSpan;
import org.knowm.xchange.service.trade.params.WithdrawFundsParams;

public class HuobiAccountService extends HuobiAccountServiceRaw implements AccountService {

  public HuobiAccountService(Exchange exchange) {
    super(exchange);
  }

  @Override
  public String withdrawFunds(WithdrawFundsParams params) throws IOException {
    if (params instanceof DefaultWithdrawFundsParams) {
      DefaultWithdrawFundsParams defaultParams = (DefaultWithdrawFundsParams) params;
      return withdrawFunds(
          defaultParams.getCurrency(), defaultParams.getAmount(), defaultParams.getAddress());
    }
    throw new IllegalStateException("Don't know how to withdraw: " + params);
  }

  @Override
  public String withdrawFunds(Currency currency, BigDecimal amount, String address)
      throws IOException {
    return String.valueOf(createWithdraw(currency.toString(), amount, null, address, null));
  }

  @Override
  public AccountInfo getAccountInfo() throws IOException {
    HuobiAccount[] accounts = getAccounts();
    if (accounts.length == 0) {
      throw new ExchangeException("Account is not recognized.");
    }
    String accountID = String.valueOf(accounts[0].getId());
    return new AccountInfo(
        accountID,
        HuobiAdapters.adaptWallet(
            HuobiAdapters.adaptBalance(getHuobiBalance(accountID).getList())));
  }

  @Override
  public TradeHistoryParams createFundingHistoryParams() {
    return new HuobiFundingHistoryParams(null, null, null);
  }

  @Override
  public List<FundingRecord> getFundingHistory(TradeHistoryParams params) throws IOException {
    String currency = null;
    if (params instanceof TradeHistoryParamCurrency
        && ((TradeHistoryParamCurrency) params).getCurrency() != null) {
      currency = ((TradeHistoryParamCurrency) params).getCurrency().getCurrencyCode();
    }

    String from = null;
    if (params instanceof TradeHistoryParamsIdSpan) {
      from = ((TradeHistoryParamsIdSpan) params).getStartId();
    }

    FundingRecord.Type type;
    if (params instanceof HistoryParamsFundingType
        && ((HistoryParamsFundingType) params).getType() != null) {
      type = ((HistoryParamsFundingType) params).getType();
    } else {
      // Funding history type is a required parameter for Huobi funding history query
      throw new ExchangeException(
          "Type 'deposit' or 'withdraw' must be supplied using FundingRecord.Type");
    }

    // Adapt type out (replace withdrawal -> withdraw)
    String fundingRecordType = type == FundingRecord.Type.WITHDRAWAL ? "withdraw" : "deposit";
    return HuobiAdapters.adaptFundingHistory(
        getDepositWithdrawalHistory(currency, fundingRecordType, from));
  }

  @Override
  public String requestDepositAddress(Currency currency, String... strings) throws IOException {
    return getDepositAddress(currency.toString());
  }

  @Override
  public AddressWithTag requestDepositAddressData(Currency currency, String... args)
      throws IOException {
    HuobiDepositAddress huobiAddrWithTag = getDepositAddressV2(currency.toString())[0];
    AddressWithTag addressWithTag =
        new AddressWithTag(huobiAddrWithTag.getAddress(), huobiAddrWithTag.getAddressTag());
    return addressWithTag;
  }

  @Override
  public Map<CurrencyPair, Fee> getDynamicTradingFeeForPairs(CurrencyPair[] currencyPairs) throws IOException {
    List<String> cps = Arrays.stream(currencyPairs).map(cp -> cp.base.toString() + cp.counter.toString()).collect(Collectors.toList());
    List<List<String>> batches = Lists.partition(cps, 10); // Batches of 10 to minimize requests to endpoint and to prevent 414 errors, Huobi seems to also have a cap to # of symbols in request

    Map<CurrencyPair, Fee> dynamicTradingFees = new HashMap<>();
    for (List<String> batch : batches) {
      String concat = StringUtils.join(batch, ",");
      HuobiTransactFeeRate[] transactFeeRates = getTransactFeeRate(concat);
      for (HuobiTransactFeeRate feeRate : transactFeeRates) {
        Fee fee = new Fee(feeRate.getActualMakerRate(), feeRate.getActualTakerRate());
        dynamicTradingFees.put(HuobiUtils.translateHuobiCurrencyPair(feeRate.getSymbol()), fee);
      }
    }
    return dynamicTradingFees;
  }
}
