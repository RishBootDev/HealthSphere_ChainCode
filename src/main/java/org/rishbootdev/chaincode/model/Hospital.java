package org.rishbootdev.chaincode.model;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;

import java.util.List;

@DataType
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Hospital {

    @Property
    private String hospitalId;
    @Property private String name;
    @Property private String address;
    @Property private List<String> doctorIds;
    @Property private List<String> patientIds;
    @Property private List<String> recordId;
    @Property private List<String> labId;


}
