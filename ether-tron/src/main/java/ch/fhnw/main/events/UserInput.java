package ch.fhnw.main.events;

import java.util.Set;

public enum UserInput {
    
    MOVE_FORWARD(265, 1), MOVE_BACKWARD(264, 2), MOVE_LEFT(263, 4), MOVE_RIGHT(262, 8);
    
    private int key;
    private byte pos;
    
    private UserInput(int key, int pos) {
        this.key = key;
        this.pos = (byte)pos;
    }
    
    
    public static boolean isInEnum(int key) {
        for (UserInput e : UserInput.values()) {
          if(e.key == key) { 
              return true; 
          }
        }
        return false;
      }
    
    
    public static UserInput getEnumByKey(int key) {
        for (UserInput e : UserInput.values()) {
            if(e.key == key) { 
                return e; 
            }
          }
          return null; 
    }
    
    public static byte toByte(Set<UserInput> userInputs) {
        byte userInput = 0;
        for(UserInput e : userInputs) {
            userInput += e.pos;
        }
        return userInput;
        
    }

}
