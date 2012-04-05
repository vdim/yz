package university.model;

public class Student {
    private String ID;
    private String name;
    
    public String getID() {
	return ID;
    }
    
    public void setID(String ID) {
	this.ID = ID;
    }
    
    public String getName() {
	return name;
    }
    
    public void setName(String name) {
	this.name = name;
    }
    
    @Override
    public String toString() {
        String name = getName();
       
        if (name == null || name.length() == 0)
            return "";
        
        return name;
    }
}
