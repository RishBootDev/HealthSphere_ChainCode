package org.rishbootdev.chaincode.model;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hyperledger.fabric.contract.annotation.Property;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LabReport {

    @Property
    @SerializedName("reportId")
    private String reportId;

    @Property
    @SerializedName("patientId")
    private String patientId;

    @Property
    @SerializedName("testType")
    private String testType;

    @Property
    @SerializedName("testResult")
    private String testResult;

    @Property
    @SerializedName("labId")
    private String labId;

    @Property
    @SerializedName("testDate")
    private String testDate;

    @Property
    @SerializedName("remarks")
    private String remarks;
}
