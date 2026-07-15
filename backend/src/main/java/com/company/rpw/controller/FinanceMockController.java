package com.company.rpw.controller;

import com.company.rpw.common.R;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

/**
 * 业务/财务模拟接口
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/finance")
public class FinanceMockController {

    /**
     * 获取项目预算信息
     * GET /api/v1/finance/budget/{projectId}
     */
    @GetMapping("/budget/{projectId}")
    public R<BudgetVO> getBudget(@PathVariable Long projectId) {
        BudgetVO budget = new BudgetVO();
        budget.setProjectId(projectId);
        budget.setTotalBudget(new BigDecimal("5000000.00"));
        budget.setUsedBudget(new BigDecimal("3200000.00"));
        budget.setRemainingBudget(new BigDecimal("1800000.00"));
        budget.setUsageRate(0.64);
        budget.setLastUpdated(LocalDate.now().toString());
        return R.ok(budget);
    }

    /**
     * 获取项目付款记录
     * GET /api/v1/finance/payment/{projectId}
     */
    @GetMapping("/payment/{projectId}")
    public R<List<PaymentVO>> getPayments(@PathVariable Long projectId) {
        PaymentVO p1 = new PaymentVO();
        p1.setPaymentId(1L);
        p1.setProjectId(projectId);
        p1.setPaymentDate("2026-04-15");
        p1.setAmount(new BigDecimal("500000"));
        p1.setPaymentType("预付款");
        p1.setStatus("已支付");
        p1.setRemark("材料预付款");

        PaymentVO p2 = new PaymentVO();
        p2.setPaymentId(2L);
        p2.setProjectId(projectId);
        p2.setPaymentDate("2026-05-01");
        p2.setAmount(new BigDecimal("800000"));
        p2.setPaymentType("进度款");
        p2.setStatus("审核中");
        p2.setRemark("一期工程进度款");

        PaymentVO p3 = new PaymentVO();
        p3.setPaymentId(3L);
        p3.setProjectId(projectId);
        p3.setPaymentDate("2026-05-10");
        p3.setAmount(new BigDecimal("300000"));
        p3.setPaymentType("结算款");
        p3.setStatus("已支付");
        p3.setRemark("设备结算款");

        return R.ok(Arrays.asList(p1, p2, p3));
    }

    /**
     * 获取项目合同信息
     * GET /api/v1/finance/contract/{projectId}
     */
    @GetMapping("/contract/{projectId}")
    public R<ContractVO> getContract(@PathVariable Long projectId) {
        ContractVO contract = new ContractVO();
        contract.setContractId("CT-2026-001");
        contract.setProjectId(projectId);
        contract.setContractName("XX项目工程总承包合同");
        contract.setContractAmount(new BigDecimal("50000000"));
        contract.setSignedDate("2026-01-15");
        contract.setPartyA("中交集团");
        contract.setPartyB("XX建筑公司");
        contract.setStatus("执行中");
        return R.ok(contract);
    }

    @Data
    public static class BudgetVO {
        private Long projectId;
        private BigDecimal totalBudget;
        private BigDecimal usedBudget;
        private BigDecimal remainingBudget;
        private Double usageRate;
        private String lastUpdated;
    }

    @Data
    public static class PaymentVO {
        private Long paymentId;
        private Long projectId;
        private String paymentDate;
        private BigDecimal amount;
        private String paymentType;
        private String status;
        private String remark;
    }

    @Data
    public static class ContractVO {
        private String contractId;
        private Long projectId;
        private String contractName;
        private BigDecimal contractAmount;
        private String signedDate;
        private String partyA;
        private String partyB;
        private String status;
    }
}
