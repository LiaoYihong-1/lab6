package Lab;
import Collection.*;
import Command.*;
import java.io.*;
import java.net.*;
import java.nio.channels.DatagramChannel;
import java.nio.channels.Selector;
import java.util.*;

public class Server {
    public static void main (String [] args) throws IOException{
        //initialize collection of people
        new CollectionsofPerson().doInitialization();
        CommandManager manager = new CommandManager();
        DatagramSocket Set = new DatagramSocket(5555);
        byte[] B = new byte[102400];
        DatagramPacket dateP = new DatagramPacket(B,0,B.length);
        Set.receive(dateP);
        ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(dateP.getData()));
        LinkedHashSet<Person> linkedHashSet = null;
        try {
            CommandPackage commandPackage =(CommandPackage) in.readObject();
            linkedHashSet = commandPackage.getLinkedHashSet();
        }catch (ClassNotFoundException C){
            C.printStackTrace();
        }
        CollectionsofPerson.setPeople(linkedHashSet);
        byte[] after = "now your collection is synchronized by file\n".getBytes();
        Set.send(new DatagramPacket(after,0,after.length,dateP.getSocketAddress()));
        Set.close();

        while (true) {
            DatagramSocket datagramSocket = new DatagramSocket(5555);
            //receive
            byte[] buffer = new byte[102400];
            DatagramPacket datagramPacket = new DatagramPacket(buffer, 0, buffer.length);
            datagramSocket.receive(datagramPacket);
            ObjectInputStream inputStream = new ObjectInputStream(new ByteArrayInputStream(datagramPacket.getData()));
            //main part
            try {
                //deal with different command
                runningCommand(inputStream,manager,datagramSocket,datagramPacket);
            }catch (ClassNotFoundException|ParaInapproException|NullException C) {
                manager.setOut(C.getMessage(),false);
            }

            //send
            byte [] data = manager.getOut().getBytes();
            DatagramPacket send = new DatagramPacket(data,data.length,datagramPacket.getSocketAddress());
            datagramSocket.send(send);
            datagramSocket.close();
        }
    }

    private static LinkedHashSet<Person> sort(LinkedHashSet<Person> linkedHashSet) throws NullException{
        LinkedHashSet<Person> newone = new LinkedHashSet<>();
        Comparator<Person> comparator;
        comparator = Comparator.comparingInt(a -> a.getId());
        linkedHashSet.stream().sorted(comparator).forEach(newone::add);
        return newone;
    }

    public static void runningCommand(ObjectInputStream inputStream,CommandManager manager,DatagramSocket datagramSocket,DatagramPacket datagramPacket)throws ClassNotFoundException,NullException,IOException{
        CommandPackage commandPackage = (CommandPackage) inputStream.readObject();
        if (commandPackage != null) {
            if (commandPackage.getList()==null) {
                AbstractCommand command = commandPackage.getAbstractCommand();
                new History().getHistory().add(command.getName() + "\n");
                if (command.getName().equalsIgnoreCase("exit")) {
                    command.execute(manager,commandPackage);
                    datagramSocket.send(new DatagramPacket(manager.getOut().getBytes(), manager.getOut().getBytes().length, datagramPacket.getSocketAddress()));
                    new Save().execute(manager, commandPackage);
                    System.exit(2);
                } else {
                    command.execute(manager, commandPackage);
                    if (command.getName().equalsIgnoreCase("updateid")) {
                        CollectionsofPerson.setPeople(sort(new CollectionsofPerson().getPeople()));
                    }
                }
            }else{
                String S = "";
                ArrayList<CommandPackage> list = commandPackage.getList();
                Iterator<CommandPackage> iterator = list.iterator();
                while(iterator.hasNext()){
                    CommandPackage used = iterator.next();
                    AbstractCommand command = used.getAbstractCommand();
                    new History().getHistory().add(command.getName()+"\n");
                    command.execute(manager,used);
                    S=S+manager.getOut()+"\n";
                }
                manager.setOut(S,false);
            }
        }
    }
}