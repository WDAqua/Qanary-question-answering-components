package eu.wdaqua.qanary.relnliod;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Scanner;

@Component
public class RemovalList {

    private ArrayList<String> filteredWordList = new ArrayList<String>();

    public RemovalList(@Value("${rel-nliod.removal.file}") final String removalFile) {
        loadList(removalFile);
    }

    public void loadList(final String removalFile) {
        System.out.println("createFilteredWordList():");
        try {
            File filename = new File(removalFile);
            Scanner in = new Scanner(new FileReader(filename.getAbsolutePath()));
            while(in.hasNext()) {
                filteredWordList.add((in.next().trim()));
            }
            in.close();

            System.out.println(filteredWordList.toString());

        }
        catch(Exception e) {
            System.out.println("createFilteredWordList: Exception "+ e.toString());
        }
    }

    public ArrayList<String> getFilteredWordList() {
        return filteredWordList;
    }

}
