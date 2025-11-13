package com.giapho.coffee_shop_backend.dto;

import jakarta.validation.constraints.NotBlank;

public class VoucherApplyRequestDTO {

    @NotBlank(message = "Voucher code is required")
    private String voucherCode;

    public String getVoucherCode() {
        return voucherCode;
    }

    public void setVoucherCode(String voucherCode) {
        this.voucherCode = voucherCode;
    }
}
