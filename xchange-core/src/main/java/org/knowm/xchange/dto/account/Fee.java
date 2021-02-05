package org.knowm.xchange.dto.account;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;

import org.knowm.xchange.exceptions.NotYetImplementedForExchangeException;

public final class Fee implements Serializable {

  private static final long serialVersionUID = -6235230375777573680L;

  @JsonProperty("maker_fee")
  private final BigDecimal makerFee;

  @JsonProperty("taker_fee")
  private final BigDecimal takerFee;

  @JsonProperty("is_percentage")
  private final Boolean isPercentage;

  public Fee(BigDecimal makerFee, BigDecimal takerFee) {
    this.makerFee = makerFee;
    this.takerFee = takerFee;
    this.isPercentage = null;
  }

  public Fee(BigDecimal makerFee, BigDecimal takerFee, Boolean isPercentage) {
    this.makerFee = makerFee;
    this.takerFee = takerFee;
    this.isPercentage = isPercentage;
  }

  public BigDecimal getMakerFee() {
    return makerFee;
  }

  public BigDecimal getTakerFee() {
    return takerFee;
  }

  public Boolean getIsPercentage() {
    return isPercentage;
  }

  public BigDecimal getMakerFeeDecimal() {
    if (isPercentage == null) {
      throw new NotYetImplementedForExchangeException(
          "isPercentage not specified for Fee, unable to discern between percentage or decimal value");
    }

    if (isPercentage) {
      // is percentage, convert to decimal
      BigDecimal hundred = new BigDecimal("100");
      return makerFee.divide(hundred, MathContext.DECIMAL32);
    } else {
      // is decimal
      return makerFee;
    }
  }

  public BigDecimal getTakerFeeDecimal() {
    if (isPercentage == null) {
      throw new NotYetImplementedForExchangeException(
          "isPercentage not specified for Fee, unable to discern between percentage or decimal value");
    }

    if (isPercentage) {
      // is percentage, convert to decimal
      BigDecimal hundred = new BigDecimal("100");
      return takerFee.divide(hundred, MathContext.DECIMAL32);
    } else {
      // is decimal
      return takerFee;
    }
  }

  public BigDecimal getMakerFeePercentage() {
    if (isPercentage == null) {
      throw new NotYetImplementedForExchangeException(
          "isPercentage not specified for Fee, unable to discern between percentage or decimal value");
    }

    if (isPercentage) {
      // is percentage
      return makerFee;
    } else {
      // is decimal, convert to percentage
      BigDecimal hundred = new BigDecimal("100");
      return makerFee.multiply(hundred, MathContext.DECIMAL32);
    }
  }

  public BigDecimal getTakerFeePercentage() {
    if (isPercentage == null) {
      throw new NotYetImplementedForExchangeException(
          "isPercentage not specified for Fee, unable to discern between percentage or decimal value");
    }

    if (isPercentage) {
      // is percentage
      return takerFee;
    } else {
      // is decimal, convert to percentage
      BigDecimal hundred = new BigDecimal("100");
      return takerFee.multiply(hundred, MathContext.DECIMAL32);
    }
  }

  @Override
  public String toString() {
    return "{" + " makerFee='" + makerFee + "'" + ", takerFee='" + takerFee + "'" + ", isPercentage='" + isPercentage
        + "'" + "}";
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    Fee other = (Fee) obj;
    return other.makerFee.equals(makerFee) && other.takerFee.equals(takerFee)
        && other.isPercentage.equals(isPercentage);
  }

  @Override
  public int hashCode() {
    return Objects.hash(makerFee, takerFee, isPercentage);
  }
  
}
