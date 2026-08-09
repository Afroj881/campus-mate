package com.campusmate.service;
import com.campusmate.model.RegistrationForm;
import com.campusmate.model.User;
import com.campusmate.repository.UserRepository;
import java.util.Locale;
import java.util.Optional;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class UserService implements UserDetailsService {
 private final UserRepository userRepository;
 private final PasswordEncoder passwordEncoder;
 public UserService(UserRepository repo,PasswordEncoder encoder){userRepository=repo;passwordEncoder=encoder;}
 @Transactional public User registerStudent(RegistrationForm form){
  String email=normalize(form.getEmail());
  if(userRepository.existsByEmailIgnoreCase(email))throw new DuplicateEmailException();
  User user=new User(); user.setName(form.getName().trim()); user.setEmail(email);
  user.setPassword(passwordEncoder.encode(form.getPassword())); user.setRole(User.Role.STUDENT);
  user.setDepartment(form.getDepartment().trim()); user.setSemester(form.getSemester());
  return userRepository.save(user);
 }
 @Transactional(readOnly=true) public Optional<User> authenticate(String email,String password){
  if(email==null||password==null)return Optional.empty();
  return userRepository.findByEmailIgnoreCase(normalize(email)).filter(u->passwordEncoder.matches(password,u.getPassword()));
 }
 @Override @Transactional(readOnly=true)
 public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
  User user=userRepository.findByEmailIgnoreCase(normalize(username)).orElseThrow(()->new UsernameNotFoundException("User not found"));
  return org.springframework.security.core.userdetails.User.withUsername(user.getEmail()).password(user.getPassword())
   .authorities(new SimpleGrantedAuthority("ROLE_"+user.getRole().name())).build();
 } @Transactional(readOnly=true) public Optional<User> findByEmail(String email){if(email==null)return Optional.empty();return userRepository.findByEmailIgnoreCase(normalize(email));}
 private String normalize(String email){return email.trim().toLowerCase(Locale.ROOT);}
 public static class DuplicateEmailException extends RuntimeException {}
}
