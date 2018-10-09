#ifndef __IPASIR4J_INCLUDED__
#define __IPASIR4J_INCLUDED__

void* ipasir4j_init(const char* solver_name);
const char* ipasir4j_signature(void* solver);
void ipasir4j_release(void* solver);
void ipasir4j_add(void* solver, int lit_or_zero);
void ipasir4j_assume(void* solver, int lit);
int ipasir4j_solve(void* solver);
int ipasir4j_val(void* solver, int lit);
int ipasir4j_failed(void* solver, int lit);
void ipasir4j_set_terminate(void* solver, void* state,
                            int (*terminate)(void* state));

void ipasir4j_add_all(void* solver, int size, int lits[]);
void ipasir4j_assume_all(void* solver, int size, int lits[]);
void ipasir4j_val_all(void* solver, int size, int assign[]);

#endif
