/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tak;

/**
 *
 * @author chaitu
 */
public class Status {
    private String msg;
    private boolean ok;
    
    Status(String m, boolean ok) {
        msg = m;
        this.ok = ok;
    }
    
    Status(boolean ok) {
        this(null, ok);
    }
    
    boolean isOk() {
        return ok;
    }
    
    String msg() {
        return msg;
    }
    
    public String toString() {
        return (ok?"OK":"NOK")+" "+msg;
    }
}
