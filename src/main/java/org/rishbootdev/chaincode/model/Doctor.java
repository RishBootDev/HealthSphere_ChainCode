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
public class Doctor {

    @Property
    @SerializedName("doctorId")
    private String doctorId;
    @Property
    @SerializedName("name")
    private String name;
    @Property
    @SerializedName("specialization")
    private String specialization;
    @Property
    @SerializedName("hospitalId")
    private String hospitalId;
    @Property
    @SerializedName("patientId")
    private List<String> patientId=new ArrayList<>();

    @Property
    @SerializedName("recordId")
    private List<String> recordId=new ArrayList<>();
    @Property
    @SerializedName("qualification")
    private String qualification;
    @Property()
    @SerializedName("contact")
    private String contact;
}
