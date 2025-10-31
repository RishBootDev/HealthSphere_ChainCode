package org.rishbootdev.chaincode.model;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hyperledger.fabric.contract.annotation.Property;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LabReport {
    @Property
    private String reportId;
    @Property private String patientId;
    @Property private String testType;
    @Property private String testResult;
    @Property private String labId;
    @Property private String testDate;
    @Property private String remarks;
}

