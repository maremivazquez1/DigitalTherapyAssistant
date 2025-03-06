import { configureStore } from "@reduxjs/toolkit";

export const store = configureStore({
  reducer: {}, // No reducers yet, but Redux is initialized
});

export type RootState = ReturnType<typeof store.getState>;
export type AppDispatch = typeof store.dispatch;