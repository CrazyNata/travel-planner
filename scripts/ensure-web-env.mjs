const variables = {
  VITE_SUPABASE_URL: process.env.VITE_SUPABASE_URL?.trim(),
  VITE_SUPABASE_PUBLISHABLE_KEY: process.env.VITE_SUPABASE_PUBLISHABLE_KEY?.trim(),
};

const missing = Object.entries(variables)
  .filter(([, value]) => !value)
  .map(([name]) => name);

if (missing.length > 0) {
  console.error(
    `Web build requires these environment variables: ${missing.join(", ")}`,
  );
  process.exit(1);
}

if (
  variables.VITE_SUPABASE_URL.includes("your-project-ref") ||
  variables.VITE_SUPABASE_PUBLISHABLE_KEY === "sb_publishable_your_key"
) {
  console.error("Web build Supabase settings still contain example placeholders.");
  process.exit(1);
}
