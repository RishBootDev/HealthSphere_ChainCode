package org.rishbootdev.chaincode.model;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@DataType
public class Doctor {

    @Property()
    private String doctorId;
    @Property()
    private String name;
    @Property()
    private String specialization;
    @Property()
    private String hospitalId;
    @Property
    private List<String> patientId;

    @Property
    private List<String> recordId;
    @Property()
    private String qualification;
    @Property()
    private String contact;
}
