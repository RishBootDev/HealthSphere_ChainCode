package org.rishbootdev.chaincode.model;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hyperledger.fabric.contract.annotation.Property;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Record {

    @Property
    private String recordId;
    @Property private String patientId;
    @Property private String doctorId;
    @Property private String hospitalId;
    @Property private String diagnosis;
    @Property private String treatment;
    @Property private String remarks;
    @Property private String visitdate;
}



