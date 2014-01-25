/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

import a_vcard.android.syncml.pim.PropertyNode;
import a_vcard.android.syncml.pim.VDataBuilder;
import a_vcard.android.syncml.pim.VNode;
import a_vcard.android.syncml.pim.vcard.VCardException;
import a_vcard.android.syncml.pim.vcard.VCardParser;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/** Example of reading vCard
 *
 * @author ripper
 */
public class ReadExample {

    //run the WriteExample first or provide your own "example.vcard"

    public static void main(String[] args) throws Exception {

        VCardParser parser = new VCardParser();
        VDataBuilder builder = new VDataBuilder();

        String file = "example.vcard";

        //read whole file to string
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                new FileInputStream(file), "UTF-8"));
        
        String vcardString = "";
        String line;
        while ((line = reader.readLine()) != null) {
            vcardString += line + "\n";
        }
        reader.close();

        //parse the string
        boolean parsed = parser.parse(vcardString, "UTF-8", builder);
        if (!parsed) {
            throw new VCardException("Could not parse vCard file: " + file);
        }

        //get all parsed contacts
        List<VNode> pimContacts = builder.vNodeList;

        //do something for all the contacts
        for (VNode contact : pimContacts) {
            ArrayList<PropertyNode> props = contact.propList;

            //contact name - FN property
            String name = null;
            for (PropertyNode prop : props) {
                if ("FN".equals(prop.propName)) {
                    name = prop.propValue;
                    //we have the name now
                    break;
                }
            }

            //similarly for other properties (N, ORG, TEL, etc)
            //...

            System.out.println("Found contact: " + name);
        }

    }
}
