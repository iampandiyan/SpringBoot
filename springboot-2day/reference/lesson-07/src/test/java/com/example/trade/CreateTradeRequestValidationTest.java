package com.example.trade;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.trade.web.TradeController.CreateTradeRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

// Bean Validation is a plain library: no Spring, no HTTP. One ParameterizedTest replaces a dozen near-identical tests.
class CreateTradeRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private int violations(String isin, String quantity, String price) {
        var req = new CreateTradeRequest(isin,
                quantity == null ? null : new BigDecimal(quantity),
                price == null ? null : new BigDecimal(price));
        return validator.validate(req).size();
    }

    @Test
    void aValidRequestHasNoViolations() {
        assertThat(violations("US0378331005", "100", "189.50")).isZero();
    }

    @ParameterizedTest(name = "ISIN \"{0}\" is rejected")
    @ValueSource(strings = {"", "   ", "us0378331005", "US037833100", "US03783310055", "US-378331005", "0378331005US"})
    void badIsinsAreRejected(String isin) {
        assertThat(violations(isin, "100", "1")).isPositive();
    }

    @ParameterizedTest(name = "quantity={0} price={1} is rejected")
    @CsvSource({"0,1", "-1,1", "1,0", "1,-0.01"})
    void nonPositiveNumbersAreRejected(String quantity, String price) {
        assertThat(violations("US0378331005", quantity, price)).isPositive();
    }

    @Test
    void missingFieldsAreRejected() {
        assertThat(violations(null, null, null)).isEqualTo(3);
    }
}
