package university.model;

public class Faculty {
    private String office;
    private String name;
    
    public String getOffice() {
	return office;
    }
    
    public void setOffice(String office) {
	this.office = office;
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
