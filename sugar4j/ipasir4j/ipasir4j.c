#include <dlfcn.h>
#include <ipasir4j.h>
#include <stdlib.h>

typedef struct context_t {
  void* ipasir;
  const char* (*ipasir_signature)();
  void* (*ipasir_init)();
  void (*ipasir_release)(void* solver);
  void (*ipasir_add)(void* solver, int lit_or_zero);
  void (*ipasir_assume)(void* solver, int lit);
  int (*ipasir_solve)(void* solver);
  int (*ipasir_val)(void* solver, int lit);
  int (*ipasir_failed)(void* solver, int lit);
  void (*ipasir_set_terminate)(void* solver, void* state,
                               int (*terminate)(void* state));
  void* solver;
} context_t;

void* ipasir4j_init(const char* solver_name) {
  context_t* res = (context_t*)malloc(sizeof(context_t));

  res->ipasir = dlopen(solver_name, RTLD_LAZY);
  if (res->ipasir == NULL) {
    free(res);
    return NULL;
  }

  res->ipasir_signature = dlsym(res->ipasir, "ipasir_signature");
  res->ipasir_init = dlsym(res->ipasir, "ipasir_init");
  res->ipasir_release = dlsym(res->ipasir, "ipasir_release");
  res->ipasir_add = dlsym(res->ipasir, "ipasir_add");
  res->ipasir_assume = dlsym(res->ipasir, "ipasir_assume");
  res->ipasir_solve = dlsym(res->ipasir, "ipasir_solve");
  res->ipasir_val = dlsym(res->ipasir, "ipasir_val");
  res->ipasir_failed = dlsym(res->ipasir, "ipasir_failed");
  res->ipasir_set_terminate = dlsym(res->ipasir, "ipasir_set_terminate");

  if (res->ipasir_signature == NULL || res->ipasir_init == NULL ||
      res->ipasir_release == NULL || res->ipasir_add == NULL ||
      res->ipasir_assume == NULL || res->ipasir_solve == NULL ||
      res->ipasir_val == NULL || res->ipasir_failed == NULL ||
      res->ipasir_set_terminate == NULL) {
    free(res);
    return NULL;
  }

  res->solver = res->ipasir_init();
  return res;
}

const char* ipasir4j_signature(void* solver) {
  context_t* context = (context_t*)solver;
  return context->ipasir_signature();
}

void ipasir4j_release(void* solver) {
  context_t* context = (context_t*)solver;
  context->ipasir_release(context->solver);
  dlclose(context->ipasir);
  free(context);
}

void ipasir4j_add(void* solver, int lit_or_zero) {
  context_t* context = (context_t*)solver;
  context->ipasir_add(context->solver, lit_or_zero);
}

void ipasir4j_assume(void* solver, int lit) {
  context_t* context = (context_t*)solver;
  context->ipasir_assume(context->solver, lit);
}

int ipasir4j_solve(void* solver) {
  context_t* context = (context_t*)solver;
  return context->ipasir_solve(context->solver);
}

int ipasir4j_val(void* solver, int lit) {
  context_t* context = (context_t*)solver;
  return context->ipasir_val(context->solver, lit);
}

int ipasir4j_failed(void* solver, int lit) {
  context_t* context = (context_t*)solver;
  return context->ipasir_failed(context->solver, lit);
}

void ipasir4j_set_terminate(void* solver, void* state,
                            int (*terminate)(void* state)) {
  context_t* context = (context_t*)solver;
  context->ipasir_set_terminate(context->solver, state, terminate);
}

void ipasir4j_add_all(void* solver, int size, int lits[]) {
  context_t* context = (context_t*)solver;
  for (int i = 0; i < size; i++) {
    context->ipasir_add(context->solver, lits[i]);
  }
}

void ipasir4j_assume_all(void* solver, int size, int lits[]) {
  context_t* context = (context_t*)solver;
  for (int i = 0; i < size; i++) {
    context->ipasir_assume(context->solver, lits[i]);
  }
}

void ipasir4j_val_all(void* solver, int size, int assign[]) {
  context_t* context = (context_t*)solver;
  for (int i = 0; i < size; i++) {
    assign[i] = context->ipasir_val(context->solver, i + 1);
  }
}

