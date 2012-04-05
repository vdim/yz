package university.model;

import java.util.HashSet;

public class Student {
    private String ID;
    private String name;
    private HashSet<Course> courses = new HashSet<Course>();
    
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
    
    public HashSet<Course> getCourses() {
	return courses;
    }

    public void addCourse(Course s) {
	courses.add(s);
    }

    @Override
    public String toString() {
        String name = getName();
       
        if (name == null || name.length() == 0)
            return "";
        
        return name;
    }
}
