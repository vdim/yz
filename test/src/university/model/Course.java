package university.model;

import java.util.HashSet;

public class Course {
    private String code;
    private String title;
    private Faculty teacher;
    private HashSet<Student> roster = new HashSet<Student>();

    public Faculty getFaculty() {
	return teacher;
    }
    
    public void setFaculty(Faculty teacher) {
	this.teacher = teacher;
    }

    public HashSet<Student> getStudents() {
	return roster;
    }

    public void addStudent(Student s) {
	roster.add(s);
    }


    public String getCode() {
	return code;
    }
    
    public void setCode(String code) {
	this.code = code;
    }
    
    public String getTitle() {
	return title;
    }
    
    public void setTitle(String title) {
	this.title = title;
    }
    
    @Override
    public String toString() {
        String title = getTitle();
       
        if (title == null || title.length() == 0)
            return "";
        
        return title;
    }
}
