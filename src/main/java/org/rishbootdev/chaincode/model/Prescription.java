package org.rishbootdev.chaincode.model;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@DataType
public class Prescription {

    @Property
    @SerializedName("prescriptionId")
    private String prescriptionId;

    @Property
    @SerializedName("patientId")
    private String patientId;

    @Property
    @SerializedName("doctorId")
    private String doctorId;

    @Property
    @SerializedName("medicineIdList")
    private List<String> medicineIdList = new ArrayList<>();

    @Property
    @SerializedName("issuedDate")
    private String issuedDate;

    @Property
    @SerializedName("remarks")
    private String remarks;
}
