


class Kiszolgáló implements Runnable {
    
    CsevegőSzerver szerver;
    
    java.net.Socket socket;

    int ID;
    
    java.io.PrintWriter kimenőCsatorna;
    boolean kiszolgál = false;
    
    public Kiszolgáló(CsevegőSzerver szerver) {
        
        this.szerver = szerver;
        new Thread(this).start();
        
    }
    
    public synchronized void kiszolgál(java.net.Socket socket) {
        this.socket = socket;
        kiszolgál = true;
        notify();
    }
    
    public void üzenet(String üzenet) {
        kimenőCsatorna.println( üzenet );
        kimenőCsatorna.flush();
    }
   
    public synchronized void run() {
        for(;;) {
            while(!kiszolgál)
                try{
                    wait();
                } catch(InterruptedException e) {}
            try {
                java.io.BufferedReader bejövőCsatorna =
                        new java.io.BufferedReader(
                        new java.io.InputStreamReader(socket.getInputStream()));
                kimenőCsatorna =
                        new java.io.PrintWriter(socket.getOutputStream());
    
                String sor = bejövőCsatorna.readLine();
                do {
                    if("vege".equals(sor))
                        break;
                    if("mennyi".equals(sor))
                        szerver.mindenkihez("A szerverez " + szerver.getCsevego() + "ember cseveg");
                    szerver.mindenkihez( "("+ ID +"): " + sor);
                    
                } while((sor = bejövőCsatorna.readLine()) != null);
                
            } catch(java.io.IOException ioE) {
                
                ioE.printStackTrace();
                
            } finally {
                
                try{
                    socket.close();
                    szerver.kiszáll(this);
                    kiszolgál = false;
                } catch(java.io.IOException ioE) {}
                
            }
        }
    }
}


public class CsevegőSzerver {

    public static final int MAX_CSEVEGŐ = 10;
    private java.util.List<Kiszolgáló> csevegők;
    private java.util.List<Kiszolgáló> nemCsevegők;

    private java.lang.String beszélgetés = "\033[H\033[2J";

    public CsevegőSzerver() {

        nemCsevegők = new java.util.ArrayList<Kiszolgáló>();
        csevegők = new java.util.ArrayList<Kiszolgáló>();

        for(int i=1; i<=MAX_CSEVEGŐ; ++i){
            Kiszolgáló sz = new Kiszolgáló(this);
            sz.ID = i;
            nemCsevegők.add(sz);
        }
        try {
            java.net.ServerSocket serverSocket =
                    new java.net.ServerSocket(2006);
            
            while(true) {
                java.net.Socket socket = serverSocket.accept();
                Kiszolgáló szál = beszáll();
                if(szál == null) {
                    java.io.PrintWriter kimenőCsatorna =
                            new java.io.PrintWriter(socket.getOutputStream());
                    kimenőCsatorna.println("A csevegő szoba tele van!");
                    socket.close();
                } else {
                    szál.kiszolgál(socket);
                }
            }
            
        } catch(java.io.IOException ioE) {
            ioE.printStackTrace();
        }

    }
    

    public int getCsevego() {

        return csevegők.size();
    }

    public synchronized Kiszolgáló beszáll() {
        
        if(!nemCsevegők.isEmpty()) {
            Kiszolgáló kiszolgáló = nemCsevegők.remove(0);
            csevegők.add(kiszolgáló);
            return kiszolgáló;
        }
        return null;
    }
   

    public synchronized void kiszáll(Kiszolgáló csevegő) {
        csevegők.remove(csevegő);
        nemCsevegők.add(csevegő);
    }
    

    public synchronized void mindenkihez(String üzenet) {

        try{
        beszélgetés += "[" + getCsevego() + "]Csevegő" + üzenet + '\n';
        }catch(Exception ex){
            for(Kiszolgáló csevegő: csevegők){
                csevegő.üzenet("Beszélgetés meghaladta a megengedett sorok számát");
            }
        }

        for(Kiszolgáló csevegő: csevegők){
            csevegő.üzenet("\033[H\033[2J");
            csevegő.üzenet(beszélgetés);
        }
    }

    public synchronized void rendszerÜzenet(String üzenet){

        beszélgetés += "Rendszer: " + üzenet + '\n';

        for(Kiszolgáló csevegő: csevegők){
            csevegő.üzenet(beszélgetés);
        }
    }

    public static void main(String [] args) {
        new CsevegőSzerver();
    }
}