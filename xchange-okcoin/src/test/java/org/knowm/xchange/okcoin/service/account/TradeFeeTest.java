package org.knowm.xchange.okcoin.service.account;

import java.io.IOException;

import org.junit.Test;
import org.knowm.xchange.Exchange;
import org.knowm.xchange.ExchangeFactory;
import org.knowm.xchange.ExchangeSpecification;
import org.knowm.xchange.okcoin.OkexExchangeV3;

public class TradeFeeTest {
    @Test
    public void getTradeFee() throws IOException {
        Exchange exchange = ExchangeFactory.INSTANCE.createExchange(OkexExchangeV3.class, "030d8dd7-963a-4655-a062-4f827a0d6912", "A3EDEE965783F9A87B880CE9FBED4E07");
        ExchangeSpecification exchangeSpecification = exchange.getExchangeSpecification();
        exchangeSpecification.setExchangeSpecificParametersItem("passphrase", "TBJx5vjBEqa2");
        exchange.applySpecification(exchangeSpecification);

        exchange.getAccountService().getDynamicTradingFees();
    }    
}
