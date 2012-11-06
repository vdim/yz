package university.model;

import java.util.HashSet;
import ru.petrsu.nest.yz.DefaultProperty;

public class Company {
    @DefaultProperty
    private String name;

    private HashSet<Faculty> staff = new HashSet<Faculty>();
    
    
    public String getName() {
	return name;
    }
    
    public void setName(String name) {
	this.name = name;
    }
    
    public HashSet<Faculty> getFaculties() {
	return staff;
    }

    public void addFaculty(Faculty f) {
	staff.add(f);
    }
}
