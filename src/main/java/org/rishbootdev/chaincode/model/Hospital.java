package org.rishbootdev.chaincode.model;


import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;

import java.util.ArrayList;
import java.util.List;

@DataType
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Hospital {

    @Property
    @SerializedName("hospitalId")
    private String hospitalId;
    @Property
    @SerializedName("name")
    private String name;
    @Property
    @SerializedName("address")
    private String address;
    @Property
    @SerializedName("doctorIds")
    private List<String> doctorIds=new ArrayList<>();
    @Property
    @SerializedName("patientIds")
    private List<String> patientIds=new ArrayList<>();
    @Property
    @SerializedName("recordId")
    private List<String> recordId=new ArrayList<>();
    @Property
    @SerializedName("labId")
    private List<String> labId=new ArrayList<>();
}
