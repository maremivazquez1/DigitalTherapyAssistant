import React, { useState, ChangeEvent, FormEvent } from 'react';

const CBTIntakeForm: React.FC = () => {
  const [formData, setFormData] = useState({
    presentingConcerns: '',
    physicalHealthRating: '',
    physicalHealthConcerns: '',
    onMedication: '',
    sleepIssues: {
      tooMuch: false,
      tooLittle: false,
      poorQuality: false,
      disturbingDreams: false,
      other: '',
    },
    sleepHours: '',
    exerciseFrequency: '',
    dietConcerns: {
      eatingLess: false,
      eatingMore: false,
      bingeing: false,
      restricting: false,
      other: '',
    },
    weightChangeDesc: '',
    alcoholUse: '',
    alcoholFrequency: '',
    drugUseFrequency: '',
    recentDepression: '',
    recentSuicidalThoughts: '',
    pastSuicidalThoughts: '',
    currentRelationship: '',
    relationshipDuration: '',
    relationshipRating: '',
    lifeChanges: '',
    symptoms: {},
    workStatus: '',
    workSatisfaction: '',
    workFulfillment: '',
    workStressors: '',
    religiousPreference: '',
    spiritualIdentity: '',
    familyMentalHealth: {
      depression: false,
      anxiety: false,
      bipolar: false,
      panicAttacks: false,
      alcoholAbuse: false,
      drugAbuse: false,
      eatingDisorder: false,
      learningDisability: false,
      trauma: false,
      domesticViolence: false,
      ocd: false,
      schizophrenia: false,
      obesity: false,
      other: '',
    },
    strengths: '',
    growthAreas: '',
    selfLikes: '',
    copingMethods: '',
    therapyGoals: '',
    additionalInfo: '',
  });

  const [success, setSuccess] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const handleChange = (e: ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>) => {
    const { name, value, type } = e.target as HTMLInputElement;
    const checked = (e.target as HTMLInputElement).checked;
    if (name.includes('.')) {
      const [group, key] = name.split('.');
      setFormData((prev: any) => ({
        ...prev,
        [group]: {
          ...prev[group],
          [key]: type === 'checkbox' ? checked : value,
        },
      }));
    } else {
      setFormData((prev: any) => ({
        ...prev,
        [name]: type === 'checkbox' ? checked : value,
      }));
    }
  };

  const handleSubmit = (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setError(null);
    setSuccess(null);

    const hasEmpty = Object.entries(formData).some(([_, val]) => typeof val === 'string' && val.trim() === '');
    if (hasEmpty) {
      setError('Please complete all fields. Use "N/A" if not applicable.');
      return;
    }

    console.log('Submitted:', formData);
    setSuccess('Form submitted successfully!');
  };

  return (
    <div data-theme="calming" className="flex flex-col items-center justify-center min-h-screen bg-base-200 p-4">
      <h1 className="text-4xl font-bold text-primary text-center">CBT Intake Form</h1>
      <p className="text-lg text-base-content/70 text-center max-w-xl mt-2">
        Please complete all fields to the best of your ability. If unsure, you can enter "N/A".
      </p>

      <div className="w-full max-w-2xl p-8 mt-8 space-y-6 bg-base-100 rounded-lg shadow-md">
        {error && <p className="text-sm text-center text-error">{error}</p>}
        {success && <p className="text-sm text-center text-success">{success}</p>}

        <form onSubmit={handleSubmit} className="space-y-6">
            <details className="collapse collapse-arrow bg-base-200">
                <summary className="collapse-title text-lg font-medium text-base-content">
                    Presenting Concerns
                </summary>
                <div className="collapse-content space-y-4 pt-4">
                    <label htmlFor="presentingConcerns" className="block text-sm font-medium text-base-content">
                    What brings you in today?
                    </label>
                    <textarea
                    id="presentingConcerns"
                    name="presentingConcerns"
                    required
                    value={formData.presentingConcerns}
                    onChange={handleChange}
                    rows={4}
                    className="mt-1 block w-full px-3 py-2 border border-base-300 rounded-md shadow-sm focus:outline-none focus:ring-primary focus:border-primary"
                    />
                </div>
            </details>
            <details className="collapse collapse-arrow bg-base-200">
                <summary className="collapse-title text-lg font-medium text-base-content">
                    General Physical Health
                </summary>
                <div className="collapse-content space-y-4 pt-4">
                    <label htmlFor="physicalHealthRating" className="block text-sm font-medium text-base-content">
                    How would you describe your physical health?
                    </label>
                    <select
                    id="physicalHealthRating"
                    name="physicalHealthRating"
                    required
                    value={formData.physicalHealthRating}
                    onChange={handleChange}
                    className="select select-bordered w-full"
                    >
                    <option value="">Select</option>
                    <option value="Poor">Poor</option>
                    <option value="Unsatisfactory">Unsatisfactory</option>
                    <option value="Satisfactory">Satisfactory</option>
                    <option value="Good">Good</option>
                    <option value="Very good">Very good</option>
                    </select>

                    <label htmlFor="physicalHealthConcerns" className="block text-sm font-medium text-base-content">
                    Please list any persistent physical symptoms or health concerns:
                    </label>
                    <textarea
                    id="physicalHealthConcerns"
                    name="physicalHealthConcerns"
                    required
                    rows={3}
                    value={formData.physicalHealthConcerns}
                    onChange={handleChange}
                    className="textarea textarea-bordered w-full"
                    />

                    <label htmlFor="onMedication" className="block text-sm font-medium text-base-content">
                    Are you currently taking any medication for physical/medical issues? If yes, please list:
                    </label>
                    <input
                    type="text"
                    id="onMedication"
                    name="onMedication"
                    required
                    value={formData.onMedication}
                    onChange={handleChange}
                    className="input input-bordered w-full"
                    />
                </div>
            </details>
            <details className="collapse collapse-arrow bg-base-200">
                <summary className="collapse-title text-lg font-medium text-base-content">
                    Sleep & Diet
                </summary>
                <div className="collapse-content space-y-4 pt-4">
                    <fieldset>
                    <legend className="text-sm font-medium mb-2">Do you have any sleep issues? (Check all that apply)</legend>
                    {[
                        { label: 'Sleep too much', key: 'tooMuch' },
                        { label: 'Sleep too little', key: 'tooLittle' },
                        { label: 'Poor quality', key: 'poorQuality' },
                        { label: 'Disturbing dreams', key: 'disturbingDreams' },
                    ].map(({ label, key }) => (
                        <label key={key} className="flex items-center space-x-2">
                        <input
                            type="checkbox"
                            name={`sleepIssues.${key}`}
                            checked={!!formData.sleepIssues[key as keyof typeof formData.sleepIssues]}
                            onChange={handleChange}
                            className="checkbox checkbox-primary"
                        />
                        <span>{label}</span>
                        </label>
                    ))}
                    <input
                        type="text"
                        name="sleepIssues.other"
                        value={formData.sleepIssues.other}
                        onChange={handleChange}
                        placeholder="Other sleep issues..."
                        className="input input-bordered w-full mt-2"
                    />
                    </fieldset>

                    <label htmlFor="sleepHours" className="block text-sm font-medium text-base-content">
                    On average, how many hours do you sleep per night?
                    </label>
                    <input
                    type="text"
                    id="sleepHours"
                    name="sleepHours"
                    required
                    value={formData.sleepHours}
                    onChange={handleChange}
                    className="input input-bordered w-full"
                    />

                    <label htmlFor="exerciseFrequency" className="block text-sm font-medium text-base-content">
                    How many times per week do you exercise?
                    </label>
                    <input
                    type="text"
                    id="exerciseFrequency"
                    name="exerciseFrequency"
                    required
                    value={formData.exerciseFrequency}
                    onChange={handleChange}
                    className="input input-bordered w-full"
                    />

                    <fieldset>
                    <legend className="text-sm font-medium mb-2">Have your eating habits changed? (Check all that apply)</legend>
                    {[
                        { label: 'Eating less', key: 'eatingLess' },
                        { label: 'Eating more', key: 'eatingMore' },
                        { label: 'Bingeing', key: 'bingeing' },
                        { label: 'Restricting', key: 'restricting' },
                    ].map(({ label, key }) => (
                        <label key={key} className="flex items-center space-x-2">
                        <input
                            type="checkbox"
                            name={`dietConcerns.${key}`}
                            checked={!!formData.dietConcerns[key as keyof typeof formData.dietConcerns]}
                            onChange={handleChange}
                            className="checkbox checkbox-primary"
                        />
                        <span>{label}</span>
                        </label>
                    ))}
                    <input
                        type="text"
                        name="dietConcerns.other"
                        value={formData.dietConcerns.other}
                        onChange={handleChange}
                        placeholder="Other eating concerns..."
                        className="input input-bordered w-full mt-2"
                    />
                    </fieldset>

                    <label htmlFor="weightChangeDesc" className="block text-sm font-medium text-base-content">
                    Have you experienced a weight change in the last 2 months? Describe:
                    </label>
                    <textarea
                    id="weightChangeDesc"
                    name="weightChangeDesc"
                    required
                    rows={2}
                    value={formData.weightChangeDesc}
                    onChange={handleChange}
                    className="textarea textarea-bordered w-full"
                    />
                </div>
            </details>
            <details className="collapse collapse-arrow bg-base-200">
                <summary className="collapse-title text-lg font-medium text-base-content">
                    Substance Use
                </summary>
                <div className="collapse-content space-y-4 pt-4">
                    <label htmlFor="alcoholUse" className="block text-sm font-medium text-base-content">
                    Do you regularly consume alcohol?
                    </label>
                    <select
                    id="alcoholUse"
                    name="alcoholUse"
                    required
                    value={formData.alcoholUse}
                    onChange={handleChange}
                    className="select select-bordered w-full"
                    >
                    <option value="">Select</option>
                    <option value="Yes">Yes</option>
                    <option value="No">No</option>
                    </select>

                    <label htmlFor="alcoholFrequency" className="block text-sm font-medium text-base-content">
                    In a typical month, how often do you have four or more drinks in a 24-hour period?
                    </label>
                    <input
                    type="text"
                    id="alcoholFrequency"
                    name="alcoholFrequency"
                    required
                    value={formData.alcoholFrequency}
                    onChange={handleChange}
                    className="input input-bordered w-full"
                    />

                    <label htmlFor="drugUseFrequency" className="block text-sm font-medium text-base-content">
                    How often do you engage in recreational drug use?
                    </label>
                    <select
                    id="drugUseFrequency"
                    name="drugUseFrequency"
                    required
                    value={formData.drugUseFrequency}
                    onChange={handleChange}
                    className="select select-bordered w-full"
                    >
                    <option value="">Select</option>
                    <option value="Daily">Daily</option>
                    <option value="Weekly">Weekly</option>
                    <option value="Monthly">Monthly</option>
                    <option value="Rarely">Rarely</option>
                    <option value="Never">Never</option>
                    </select>
                </div>
            </details>
            <details className="collapse collapse-arrow bg-base-200">
                <summary className="collapse-title text-lg font-medium text-base-content">
                    Mood & Suicide Risk
                </summary>
                <div className="collapse-content space-y-4 pt-4">
                    <label htmlFor="recentDepression" className="block text-sm font-medium text-base-content">
                    Have you felt depressed recently? If yes, for how long?
                    </label>
                    <input
                    type="text"
                    id="recentDepression"
                    name="recentDepression"
                    required
                    value={formData.recentDepression}
                    onChange={handleChange}
                    className="input input-bordered w-full"
                    />

                    <label htmlFor="recentSuicidalThoughts" className="block text-sm font-medium text-base-content">
                    Have you had suicidal thoughts recently? If yes, how often?
                    </label>
                    <select
                    id="recentSuicidalThoughts"
                    name="recentSuicidalThoughts"
                    required
                    value={formData.recentSuicidalThoughts}
                    onChange={handleChange}
                    className="select select-bordered w-full"
                    >
                    <option value="">Select</option>
                    <option value="Frequently">Frequently</option>
                    <option value="Sometimes">Sometimes</option>
                    <option value="Rarely">Rarely</option>
                    <option value="Never">Never</option>
                    </select>

                    <label htmlFor="pastSuicidalThoughts" className="block text-sm font-medium text-base-content">
                    Have you had suicidal thoughts in the past? If yes, how long ago and how often?
                    </label>
                    <input
                    type="text"
                    id="pastSuicidalThoughts"
                    name="pastSuicidalThoughts"
                    required
                    value={formData.pastSuicidalThoughts}
                    onChange={handleChange}
                    className="input input-bordered w-full"
                    />
                </div>
            </details>
            <details className="collapse collapse-arrow bg-base-200">
                <summary className="collapse-title text-lg font-medium text-base-content">
                    Relationship & Life Changes
                </summary>
                <div className="collapse-content space-y-4 pt-4">
                    <label htmlFor="currentRelationship" className="block text-sm font-medium text-base-content">
                    Are you currently in a romantic relationship?
                    </label>
                    <select
                    id="currentRelationship"
                    name="currentRelationship"
                    required
                    value={formData.currentRelationship}
                    onChange={handleChange}
                    className="select select-bordered w-full"
                    >
                    <option value="">Select</option>
                    <option value="Yes">Yes</option>
                    <option value="No">No</option>
                    </select>

                    <label htmlFor="relationshipDuration" className="block text-sm font-medium text-base-content">
                    If yes, how long have you been in this relationship?
                    </label>
                    <input
                    type="text"
                    id="relationshipDuration"
                    name="relationshipDuration"
                    required
                    value={formData.relationshipDuration}
                    onChange={handleChange}
                    className="input input-bordered w-full"
                    />

                    <label htmlFor="relationshipRating" className="block text-sm font-medium text-base-content">
                    On a scale from 1 to 10, how would you rate the quality of this relationship?
                    </label>
                    <input
                    type="number"
                    id="relationshipRating"
                    name="relationshipRating"
                    min={1}
                    max={10}
                    required
                    value={formData.relationshipRating}
                    onChange={handleChange}
                    className="input input-bordered w-full"
                    />

                    <label htmlFor="lifeChanges" className="block text-sm font-medium text-base-content">
                    In the last year, have you experienced any major life changes (e.g., employment, relocation, illness)?
                    </label>
                    <textarea
                    id="lifeChanges"
                    name="lifeChanges"
                    required
                    rows={3}
                    value={formData.lifeChanges}
                    onChange={handleChange}
                    className="textarea textarea-bordered w-full"
                    />
                </div>
            </details>

            <details className="collapse collapse-arrow bg-base-200">
                <summary className="collapse-title text-lg font-medium text-base-content">
                    Work & Spiritual Life
                </summary>
                <div className="collapse-content space-y-4 pt-4">
                    <label htmlFor="workStatus" className="block text-sm font-medium text-base-content">
                    Are you currently employed or in school?
                    </label>
                    <select
                    id="workStatus"
                    name="workStatus"
                    required
                    value={formData.workStatus}
                    onChange={handleChange}
                    className="select select-bordered w-full"
                    >
                    <option value="">Select</option>
                    <option value="Employed">Employed</option>
                    <option value="Unemployed">Unemployed</option>
                    <option value="In School">In School</option>
                    <option value="Other">Other</option>
                    </select>

                    <label htmlFor="workSatisfaction" className="block text-sm font-medium text-base-content">
                    Are you happy in your current position?
                    </label>
                    <select
                    id="workSatisfaction"
                    name="workSatisfaction"
                    required
                    value={formData.workSatisfaction}
                    onChange={handleChange}
                    className="select select-bordered w-full"
                    >
                    <option value="">Select</option>
                    <option value="Yes">Yes</option>
                    <option value="No">No</option>
                    </select>

                    <label htmlFor="workFulfillment" className="block text-sm font-medium text-base-content">
                    Do you feel fulfilled in your current position?
                    </label>
                    <select
                    id="workFulfillment"
                    name="workFulfillment"
                    required
                    value={formData.workFulfillment}
                    onChange={handleChange}
                    className="select select-bordered w-full"
                    >
                    <option value="">Select</option>
                    <option value="Yes">Yes</option>
                    <option value="No">No</option>
                    </select>

                    <label htmlFor="workStressors" className="block text-sm font-medium text-base-content">
                    What aspects of your work or school cause stress or bring you joy?
                    </label>
                    <textarea
                    id="workStressors"
                    name="workStressors"
                    required
                    rows={3}
                    value={formData.workStressors}
                    onChange={handleChange}
                    className="textarea textarea-bordered w-full"
                    />

                    <label htmlFor="religiousPreference" className="block text-sm font-medium text-base-content">
                    Do you practice or observe a religion? If yes, what is your faith?
                    </label>
                    <input
                    type="text"
                    id="religiousPreference"
                    name="religiousPreference"
                    required
                    value={formData.religiousPreference}
                    onChange={handleChange}
                    className="input input-bordered w-full"
                    />

                    <label htmlFor="spiritualIdentity" className="block text-sm font-medium text-base-content">
                    If not religious, do you consider yourself spiritual? How would you describe it?
                    </label>
                    <input
                    type="text"
                    id="spiritualIdentity"
                    name="spiritualIdentity"
                    required
                    value={formData.spiritualIdentity}
                    onChange={handleChange}
                    className="input input-bordered w-full"
                    />
                </div>
            </details>

            <details className="collapse collapse-arrow bg-base-200">
                <summary className="collapse-title text-lg font-medium text-base-content">
                    Family Mental Health History
                </summary>
                <div className="collapse-content space-y-4 pt-4">
                    {[
                    { label: 'Depression', key: 'depression' },
                    { label: 'Anxiety Disorders', key: 'anxiety' },
                    { label: 'Bipolar Disorder', key: 'bipolar' },
                    { label: 'Panic Attacks', key: 'panicAttacks' },
                    { label: 'Alcohol Abuse', key: 'alcoholAbuse' },
                    { label: 'Drug Abuse', key: 'drugAbuse' },
                    { label: 'Eating Disorder', key: 'eatingDisorder' },
                    { label: 'Learning Disability', key: 'learningDisability' },
                    { label: 'Trauma', key: 'trauma' },
                    { label: 'Domestic Violence', key: 'domesticViolence' },
                    { label: 'OCD', key: 'ocd' },
                    { label: 'Schizophrenia', key: 'schizophrenia' },
                    { label: 'Obesity', key: 'obesity' },
                    ].map(({ label, key }) => (
                    <label key={key} className="flex items-center space-x-2">
                        <input
                        type="checkbox"
                        name={`familyMentalHealth.${key}`}
                        checked={!!formData.familyMentalHealth[key as keyof typeof formData.familyMentalHealth]}
                        onChange={handleChange}
                        className="checkbox checkbox-primary"
                        />
                        <span>{label}</span>
                    </label>
                    ))}

                    <label htmlFor="familyMentalHealth.other" className="block text-sm font-medium text-base-content mt-4">
                    Other concerns:
                    </label>
                    <input
                    type="text"
                    id="familyMentalHealth.other"
                    name="familyMentalHealth.other"
                    value={formData.familyMentalHealth.other}
                    onChange={handleChange}
                    className="input input-bordered w-full"
                    />
                </div>
            </details>

            <details className="collapse collapse-arrow bg-base-200">
                <summary className="collapse-title text-lg font-medium text-base-content">
                    Strengths & Goals
                </summary>
                <div className="collapse-content space-y-4 pt-4">
                    <label htmlFor="strengths" className="block text-sm font-medium text-base-content">
                    List your strengths:
                    </label>
                    <textarea
                    id="strengths"
                    name="strengths"
                    required
                    rows={2}
                    value={formData.strengths}
                    onChange={handleChange}
                    className="textarea textarea-bordered w-full"
                    />

                    <label htmlFor="growthAreas" className="block text-sm font-medium text-base-content">
                    List areas you'd like to develop or improve:
                    </label>
                    <textarea
                    id="growthAreas"
                    name="growthAreas"
                    required
                    rows={2}
                    value={formData.growthAreas}
                    onChange={handleChange}
                    className="textarea textarea-bordered w-full"
                    />

                    <label htmlFor="selfLikes" className="block text-sm font-medium text-base-content">
                    What do you like most about yourself?
                    </label>
                    <textarea
                    id="selfLikes"
                    name="selfLikes"
                    required
                    rows={2}
                    value={formData.selfLikes}
                    onChange={handleChange}
                    className="textarea textarea-bordered w-full"
                    />

                    <label htmlFor="copingMethods" className="block text-sm font-medium text-base-content">
                    How do you typically cope with stress or life obstacles?
                    </label>
                    <textarea
                    id="copingMethods"
                    name="copingMethods"
                    required
                    rows={2}
                    value={formData.copingMethods}
                    onChange={handleChange}
                    className="textarea textarea-bordered w-full"
                    />

                    <label htmlFor="therapyGoals" className="block text-sm font-medium text-base-content">
                    What are your goals for therapy?
                    </label>
                    <textarea
                    id="therapyGoals"
                    name="therapyGoals"
                    required
                    rows={3}
                    value={formData.therapyGoals}
                    onChange={handleChange}
                    className="textarea textarea-bordered w-full"
                    />

                    <label htmlFor="additionalInfo" className="block text-sm font-medium text-base-content">
                    Is there anything else you'd like to share?
                    </label>
                    <textarea
                    id="additionalInfo"
                    name="additionalInfo"
                    required
                    rows={3}
                    value={formData.additionalInfo}
                    onChange={handleChange}
                    className="textarea textarea-bordered w-full"
                    />
                </div>
            </details>



          <div className="flex justify-center pt-4">
            <button type="submit" className="btn btn-wide btn-primary">
              Submit Intake Form
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default CBTIntakeForm;
