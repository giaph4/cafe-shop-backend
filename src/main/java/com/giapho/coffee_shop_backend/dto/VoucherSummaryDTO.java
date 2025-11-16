package com.giapho.coffee_shop_backend.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class VoucherSummaryDTO {
    private long activeCount;
    private long inactiveCount;
    private long expiringSoonCount;
    private long redeemedCount;
}
