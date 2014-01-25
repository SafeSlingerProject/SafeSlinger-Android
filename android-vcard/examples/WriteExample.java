/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

import a_vcard.android.provider.Contacts;
import a_vcard.android.syncml.pim.vcard.ContactStruct;
import a_vcard.android.syncml.pim.vcard.VCardComposer;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;

/** Example of writing vCard
 *
 * @author ripper
 */
public class WriteExample {

    public static void main(String[] args) throws Exception {

        OutputStreamWriter writer = new OutputStreamWriter(
                new FileOutputStream("example.vcard"), "UTF-8");

        VCardComposer composer = new VCardComposer();

        //create a contact
        ContactStruct contact1 = new ContactStruct();
        contact1.name = "Neo";
        contact1.company = "The Company";
        contact1.addPhone(Contacts.Phones.TYPE_MOBILE, "+123456789", null, true);

        //create vCard representation
        String vcardString = composer.createVCard(contact1, VCardComposer.VERSION_VCARD30_INT);

        //write vCard to the output stream
        writer.write(vcardString);
        writer.write("\n"); //add empty lines between contacts

        // repeat for other contacts
        // ...

        writer.close();
    }
}
