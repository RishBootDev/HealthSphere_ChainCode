package org.rishbootdev.chaincode.model;


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
    private String patientId;
    @Property
    private String name;
    @Property
    private int age;
    @Property
    private String gender;
    @Property
    private String address;
    @Property
    private String labReportId;
    @Property
    private String contact;
    @Property
    private String bloodGroup;
    @Property
    private String allergies;
    @Property
    private String hospitalId;
}
