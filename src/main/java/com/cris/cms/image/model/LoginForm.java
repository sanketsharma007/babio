package com.cris.cms.image.model;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginForm {
    private String method;
    private String back;
    private String ok;
    private String output;
    private String crewid = "TEST";
    private String crewname;
    private String crewdivision;
    private String crewzone;
    private String finger;
    private String first_finger;
    private String second_finger;
    private String reregistration = "false";
    private String timeout = "30";
    private String barepeat = "false";
    private String crewstatus;
    private String signonid;
    private String camstatus;
}
