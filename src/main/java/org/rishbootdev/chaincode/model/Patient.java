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
public class Patient {

    @Property
    @SerializedName("patientId")
    private String patientId;

    @Property
    @SerializedName("name")
    private String name;

    @Property
    @SerializedName("age")
    private int age;

    @Property
    @SerializedName("gender")
    private String gender;

    @Property
    @SerializedName("address")
    private String address;

    @Property
    @SerializedName("labReportId")
    private String labReportId;

    @Property
    @SerializedName("contact")
    private String contact;

    @Property
    @SerializedName("bloodGroup")
    private String bloodGroup;

    @Property
    @SerializedName("allergies")
    private String allergies;

    @Property
    @SerializedName("hospitalId")
    private String hospitalId;
}
