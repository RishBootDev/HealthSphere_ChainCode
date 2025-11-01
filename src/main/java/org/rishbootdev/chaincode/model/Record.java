package org.rishbootdev.chaincode.model;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;

@Data
@NoArgsConstructor
@AllArgsConstructor
@DataType
public class Record {

    @Property
    @SerializedName("recordId")
    private String recordId;

    @Property
    @SerializedName("patientId")
    private String patientId;

    @Property
    @SerializedName("doctorId")
    private String doctorId;

    @Property
    @SerializedName("hospitalId")
    private String hospitalId;

    @Property
    @SerializedName("diagnosis")
    private String diagnosis;

    @Property
    @SerializedName("treatment")
    private String treatment;

    @Property
    @SerializedName("remarks")
    private String remarks;

    @Property
    @SerializedName("visitDate")
    private String visitDate;
}
