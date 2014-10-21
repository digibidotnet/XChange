package com.xeiam.xchange.coinsetter.dto.marketdata;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.math.BigDecimal;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class CoinsetterQuoteTest {

  private final ObjectMapper mapper = new ObjectMapper();

  @Test
  public void test() throws JsonParseException, JsonMappingException, IOException {

    CoinsetterQuote quote = mapper.readValue(getClass().getResource("quote.json"), CoinsetterQuote.class);
    assertEquals("BTCUSD", quote.getSymbol());
    assertEquals(new BigDecimal("10.0"), quote.getQuantity());
    assertEquals(new BigDecimal("607.32"), quote.getVwapPrice());
    assertEquals("USD", quote.getVwapCurrency());
    assertEquals(new BigDecimal("6073.27"), quote.getTotalPrice());
  }

}
