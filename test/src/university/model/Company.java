package university.model;

import java.util.HashSet;
import ru.petrsu.nest.yz.DefaultProperty;

public class Company {
    @DefaultProperty
    private String name;

    private HashSet<Person> staff = new HashSet<Person>();
    
    
    public String getName() {
	return name;
    }
    
    public void setName(String name) {
	this.name = name;
    }
    
    public HashSet<Person> getFaculties() {
	return staff;
    }

    public void addFaculty(Person f) {
	staff.add(f);
    }
}
