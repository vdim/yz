package university.model;

import java.util.HashSet;

public class Student extends Person {
    private String ID;
    private HashSet<Course> courses = new HashSet<Course>();
    
    public String getID() {
	return ID;
    }
    
    public void setID(String ID) {
	this.ID = ID;
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
