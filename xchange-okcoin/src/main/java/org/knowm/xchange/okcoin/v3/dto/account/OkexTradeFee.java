package org.knowm.xchange.okcoin.v3.dto.account;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import org.knowm.xchange.okcoin.v3.dto.trade.OkexResponse;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
@JsonInclude(Include.NON_NULL)
public class OkexTradeFee extends OkexResponse {

  private String category;
  private BigDecimal maker;
  private BigDecimal taker;
  private String timestamp;
}
