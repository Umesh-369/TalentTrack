package com.umesh.talenttrack.security;

import com.umesh.talenttrack.domain.Candidate;
import com.umesh.talenttrack.domain.Recruiter;
import com.umesh.talenttrack.domain.UserType;
import com.umesh.talenttrack.repository.CandidateRepository;
import com.umesh.talenttrack.repository.RecruiterRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final RecruiterRepository recruiterRepository;
    private final CandidateRepository candidateRepository;

    public CustomUserDetailsService(RecruiterRepository recruiterRepository, CandidateRepository candidateRepository) {
        this.recruiterRepository = recruiterRepository;
        this.candidateRepository = candidateRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // Try looking up Recruiter first
        Optional<Recruiter> recruiterOpt = recruiterRepository.findByEmail(email);
        if (recruiterOpt.isPresent()) {
            Recruiter recruiter = recruiterOpt.get();
            String roleName = "ROLE_" + recruiter.getRole().name(); // ROLE_RECRUITER or ROLE_ADMIN
            return new CustomUserDetails(
                    recruiter.getId(),
                    recruiter.getEmail(),
                    recruiter.getPasswordHash(),
                    UserType.RECRUITER,
                    recruiter.getCompany().getId(),
                    List.of(new SimpleGrantedAuthority(roleName))
            );
        }

        // Try looking up Candidate next
        Optional<Candidate> candidateOpt = candidateRepository.findByEmail(email);
        if (candidateOpt.isPresent()) {
            Candidate candidate = candidateOpt.get();
            return new CustomUserDetails(
                    candidate.getId(),
                    candidate.getEmail(),
                    candidate.getPasswordHash(),
                    UserType.CANDIDATE,
                    null, // candidates do not have a companyId
                    List.of(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
            );
        }

        throw new UsernameNotFoundException("User not found with email: " + email);
    }
}
