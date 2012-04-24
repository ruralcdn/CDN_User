package NewStack;

import java.io.*;

public class FindDrive
{
/**
 * Application Entry Point
 */
public static void main(String[] args)
    {
    String[] letters = new String[]{ "A", "B", "C", "D", "E", "F", "G", "H", "I"};
    File[] drives = new File[letters.length];
    boolean[] isDrive = new boolean[letters.length];

    // init the file objects and the initial drive state
    for ( int i = 0; i < letters.length; ++i )
        {
        drives[i] = new File(letters[i]+":/");

        isDrive[i] = drives[i].canRead();
        }

     System.out.println("FindDrive: waiting for devices...");

     // loop indefinitely
     while(true)
        {
        // check each drive
        for ( int i = 0; i < letters.length; ++i )
            {
            boolean pluggedIn = drives[i].canRead();

            // if the state has changed output a message
            if ( pluggedIn != isDrive[i] )
                {
                if ( pluggedIn )
                    System.out.println("Drive "+letters[i]+" has been plugged in");
                else
                    System.out.println("Drive "+letters[i]+" has been unplugged");

                isDrive[i] = pluggedIn;
                }
            }

        // wait before looping
        try { Thread.sleep(100); }
        catch (InterruptedException e) { /* do nothing */ }

        }
    }
}
